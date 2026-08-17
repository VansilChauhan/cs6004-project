package devirtualizer.PTAToolkit;

import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import soot.Local;
import soot.SootField;

public class PointsToState {
    private Map<Local, PointsToSet> stack = new HashMap<>();
    private Map<HeapObject, Map<ObjectField, PointsToSet>> heap = new HashMap<>();
    private Set<HeapObject> escapingSet = new HashSet<>();

    public PointsToState() {}
    public PointsToState(Set<Local> locals, Set<HeapObject> objects) {
        for(Local local : locals) {
            stack.put(local, new PointsToSet(UndefinedObject.getInstance()));
        }
        for(HeapObject obj : objects) {
            heap.put(obj, new HashMap<>());
            for(ObjectField field : Util.getFields(obj.getSootClass())) {
                heap.get(obj).put(field, new PointsToSet(NullObject.getInstance()));
            }
        }
        for(HeapObject obj : objects) {
            if(obj instanceof ThisObject || obj instanceof ParameterObject) {
                escapingSet.add(obj);
            }
        }
    }
    public PointsToState(PointsToState pt_state) {
        stack = new HashMap<>(pt_state.stack);
        heap = new HashMap<>(pt_state.heap);
        escapingSet = new HashSet<>(pt_state.escapingSet);
    }

    public void copy(PointsToState pt_state) {
        stack.clear();
        heap.clear();
        escapingSet.clear();

        for(Map.Entry<Local, PointsToSet> entry : pt_state.stack.entrySet()) {
            stack.put(entry.getKey(), entry.getValue().copy());
        }

        for(Map.Entry<HeapObject, Map<ObjectField, PointsToSet>> entry : pt_state.heap.entrySet()) {
            Map<ObjectField, PointsToSet> new_field_map = new HashMap<>();
            
            for(Map.Entry<ObjectField, PointsToSet> field_entry : entry.getValue().entrySet()) {
                new_field_map.put(field_entry.getKey(), field_entry.getValue().copy());
            }

            heap.put(entry.getKey(), new_field_map);
        }

        escapingSet.addAll(pt_state.escapingSet);
    }

    public void copyHeap(PointsToState pt_state) {
        heap = new HashMap<>(pt_state.heap);
    }

    public void copyEscapeSet(PointsToState pt_state) {
        escapingSet = new HashSet<>(pt_state.escapingSet);
    }
 
    public void merge(PointsToState pt_state1, PointsToState pt_state2) {
        stack = new HashMap<>(pt_state1.stack);
        heap = new HashMap<>(pt_state1.heap);
        escapingSet = new HashSet<>(pt_state1.escapingSet);

        mergeStack(pt_state2.stack);
        mergeHeap(pt_state2);
        // escapingSet.addAll(pt_state2.escapingSet);
        mergeEscapeSet(pt_state2);  
    }

    public void update(Local local, PointsToSet pts) {
        stack.put(local, pts);
    }

    public void update(Local dst, Local src) {
        stack.put(dst, stack.get(src).copy());
    }

    public void strongUpdate(HeapObject obj, ObjectField sf, PointsToSet pts) {
        heap.get(obj).put(sf, pts);
    }

    public void weakUpdate(HeapObject obj, ObjectField sf, PointsToSet pts) {
        heap.get(obj).get(sf).addAll(pts);
    }


    public PointsToSet getPointeeSet(Local local) {
        return stack.get(local).copy();
    }

    public PointsToSet getPointeeSet(HeapObject obj) {
        PointsToSet result = new PointsToSet();
        if(!heap.containsKey(obj))
            return result;
        for(PointsToSet pts : heap.get(obj).values()) {
            result.addAll(pts);
        }
        return result;
    }

    public PointsToSet getPointeeSet(HeapObject obj, ObjectField sf) {
        if(!heap.containsKey(obj))
            return new PointsToSet();
        if(!heap.get(obj).containsKey(sf))
            return new PointsToSet();
        return heap.get(obj).get(sf);
    }

    public void escape(PointsToSet rootPointsToSet, PointsToState in) {
        Set<HeapObject> rootSet = rootPointsToSet.getHeapObjects();

        Queue<HeapObject> que = new ArrayDeque<>();
        que.addAll(rootSet);
        escapingSet.addAll(rootSet);

        while(!que.isEmpty()) {
            HeapObject obj = que.poll();

            for(ObjectField sf : Util.getFields(obj.getSootClass())) {
                Set<HeapObject> pts = in.getPointeeSet(obj, sf).getHeapObjects();
                
                for(HeapObject next : pts) {
                    if(escapingSet.contains(next) ) 
                        continue;

                    escapingSet.add(next);
                    que.add(next);
                }
            }
        }
    }

    public Set<HeapObject> getEscapeSet() {
        return new HashSet<>(escapingSet);
    }

    public boolean doesEscape(HeapObject obj) {
        return escapingSet.contains(obj);
    }

    @Override 
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\tstack: {\n");
        for(Local local : stack.keySet()) {
            PointsToSet pts = stack.get(local);
            if(pts.size() == 1 && pts.contains(UndefinedObject.getInstance()))
                continue;
            sb.append("\t" + local + ": " + pts + ", \n");
        }
        sb.append("\t}\n");

        sb.append("\theap: {\n");
        for(HeapObject obj : heap.keySet()) {
            if(hasDefaultState(obj))    continue;

            sb.append("\t\t" + obj + "{\n");
            for(ObjectField sf : heap.get(obj).keySet()) {
                PointsToSet pts = heap.get(obj).get(sf);
                if(pts.size() == 1 && pts.contains(NullObject.getInstance()))
                    continue;
                sb.append("\t\t\t" + sf + ": " + pts + "\n");
            }
            sb.append("\t\t}\n");
        }
        sb.append("\t}\n");

        sb.append("\tescaping Set: " + escapingSet + "\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)     return false;
        if(!(obj instanceof PointsToState))     return false;
        if(obj == this)     return true;

        PointsToState ptstate = (PointsToState) obj;
        return ptstate.stack.equals(stack) && 
            ptstate.heap.equals(heap) && 
            ptstate.escapingSet.equals(escapingSet);
    }

    @Override
    public int hashCode() {
        return stack.hashCode() * 31 + heap.hashCode();
    }

    /* private methods for help */
    private void mergeStack(Map<Local, PointsToSet> src_stack) {
        for(Map.Entry<Local, PointsToSet> entry : src_stack.entrySet()) {
            // stack.put(entry.getKey(), entry.getValue().copy());
            if(!stack.containsKey(entry.getKey())) {
                stack.put(entry.getKey(), new PointsToSet());
            }
            stack.get(entry.getKey()).addAll(entry.getValue().copy());
        }
    }

    public void mergeHeap(PointsToState src) {
        for(Map.Entry<HeapObject, Map<ObjectField, PointsToSet>> entry : src.heap.entrySet()) {
            if(!heap.containsKey(entry.getKey())) {
                heap.put(entry.getKey(), new HashMap<>());
            }

            HeapObject object = entry.getKey();

            for(Map.Entry<ObjectField, PointsToSet> field_entry : entry.getValue().entrySet()) {
                if(!heap.get(object).containsKey(field_entry.getKey())) {
                    heap.get(object).put(field_entry.getKey(), new PointsToSet());
                }

                heap.get(object).get(field_entry.getKey()).addAll(field_entry.getValue().copy());
            }
        }
    }

    public void mergeEscapeSet(PointsToState src) {
        escapingSet.addAll(src.escapingSet);
    }

    private boolean hasDefaultState(HeapObject obj) {
        if(!heap.containsKey(obj))   return true;
        for(ObjectField field : heap.get(obj).keySet()) {
            PointsToSet pts = heap.get(obj).get(field);
            if(pts.size() != 1 || !pts.contains(NullObject.getInstance()))
                return false;
        }
        return true;
    }
    
}
