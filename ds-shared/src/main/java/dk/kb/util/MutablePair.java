package dk.kb.util;

public class MutablePair<L,R> {

    L left;
    R right;

    public MutablePair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }
    public L getKey() {
        return left;
    }

    public R getRight() {
        return right;
    }
    public R getValue() {
        return right;
    }

    public void setLeft(L left) {
        this.left = left;
    }
    public void setKey(L key) {
        this.left = key;
    }

    public void setRight(R right) {
        this.right = right;
    }
    public void setValue(R value) {
        this.right = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MutablePair)) {
            return false;
        }

        MutablePair<?, ?> pair = (MutablePair<?, ?>) o;

        if (left != null ? !left.equals(pair.left) : pair.left != null) {
            return false;
        }
        return !(right != null ? !right.equals(pair.right) : pair.right != null);

    }

    @Override
    public int hashCode() {
        int result = left != null ? left.hashCode() : 0;
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }
}
