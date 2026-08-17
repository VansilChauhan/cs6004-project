package devirtualizer.PTAToolkit;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PointsToSet {
    private Set<AbstractObject> set = new HashSet<>();
    public static int maxSize;

    public PointsToSet() {}
    public PointsToSet(AbstractObject obj) {
        set.add(obj);
    }
    public PointsToSet(Set<HeapObject> in_set) {
        set.addAll(in_set);
    }

    public boolean isSingleton() {
        boolean found = false;
        for(AbstractObject obj : set) {
            if(obj instanceof HeapObject) {
                if(found)   return false;
                found  = true;
            }
        }
        return found;
    }

    public boolean isBottom() {
        return set.size() == maxSize;
    }

    public void add(AbstractObject obj) {
        set.add(obj);
    }

    public void addAll(PointsToSet pts) {
        set.addAll(pts.set);
    }

    public boolean contains(AbstractObject obj) {
        return set.contains(obj);
    }

    public PointsToSet copy() {
        PointsToSet pts = new PointsToSet();
        pts.set = new HashSet<>(set);
        return pts;
    }

    public int size() {
        return set.size();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)     return false;
        if(!(obj instanceof PointsToSet))     return false;
        if(obj == this)     return true;

        return ((PointsToSet) obj).set.equals(set);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    @Override
    public String toString() {
        if(isBottom()) {
            return "BOTTOM";
        }
        return set.toString();
    }

    public Set<HeapObject> getHeapObjects() {
        return set.stream()
            .filter(o -> o instanceof HeapObject)
            .map(o -> (HeapObject) o)
            .collect(Collectors.toSet());
    }
}