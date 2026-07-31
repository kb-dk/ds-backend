package dk.kb.util.yaml;

@FunctionalInterface
public interface YAMLVisitor {

    abstract public void visit(Object yaml);



}


