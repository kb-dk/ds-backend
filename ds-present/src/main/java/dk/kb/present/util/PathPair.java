package dk.kb.present.util;

import java.util.List;

/**
 * Pair object for storing a pair of path and value.
 * Almost identical with the {@link dk.kb.util.Pair}-class. The only difference is that Path and Value can be set through a setter in PathPair.
 *
 * @param <Path>  representing a path.
 * @param <Value> represents the value present at given path.
 */
public class PathPair<Path, Value> {

    private Path path;
    private Value value;

    public PathPair(Path path, Value value) {
        this.path = path;
        this.value = value;
    }

    public Path getPath() {
        return path;
    }

    public Path getKey() {
        return path;
    }

    public Value getValue() {
        return value;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PathPair)) {
            return false;
        }

        PathPair<?, ?> pair = (PathPair<?, ?>) o;

        if (path != null ? !path.equals(pair.getPath()) : pair.getPath() != null) {
            return false;
        }
        if (value != null ? !value.equals(pair.getValue()) : pair.getValue() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PathPair{" +
                "path=" + path +
                ", value=" + value +
                '}';
    }
}
