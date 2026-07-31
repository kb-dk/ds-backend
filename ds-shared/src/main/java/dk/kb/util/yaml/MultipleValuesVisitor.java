package dk.kb.util.yaml;

import java.util.ArrayList;
import java.util.List;

public class MultipleValuesVisitor implements YAMLVisitor {
    List<Object> extractedValues = new ArrayList<>();

    public MultipleValuesVisitor(){}

    @Override
    public void visit(Object yamlEntry) {
        this.extractedValues.add(yamlEntry);
    }
}
