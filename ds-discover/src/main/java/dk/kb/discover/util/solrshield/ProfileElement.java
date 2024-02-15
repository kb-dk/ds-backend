/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.discover.util.solrshield;

import java.util.List;

/**
 * Implementors of this interface must create a deep copy, fully independent of the original object.
 */
public abstract class ProfileElement<T> implements Cloneable {

    /**
     * All profile elements are aware of the profile they belong to.
     */
    protected Profile profile;

    /**
     * The name of the profile element, typically used as keys for maps or for debug.
     */
    protected String name;

    /**
     * Construct an element belonging to the given {@code profile} and with the given {@code name}.
     * @param profile where the element belongs.
     * @param name    id/designation of the element.
     */
    public ProfileElement(Profile profile, String name) {
        this.profile = profile;
        this.name = name;
    }

    /**
     * Perform a copy of this object, ensuring that the copy is fully independent.
     * @param profile the profile that this deep copy should belong to.
     * @return a fully independent copy of this object.
     */
    @SuppressWarnings("unchecked")
    public T deepCopy(Profile profile) {
        T clone;
        try {
            clone = (T) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(
                    "Got CloneNotSupportedException with super class Object. This should not happen", e);
        } catch (ClassCastException e) {
            throw new IllegalStateException(
                    "ClassCastException probably caused by implementing class T not extending ProfileElement<T>", e);
        }
        ((ProfileElement<T>)clone).profile = profile;

        deepCopyNonAtomicAttributes(clone);
        return clone;
    }

    /**
     * Inheriting classes must override this (with call to {@code super()}) to deep copy attributes that are not atomic.
     * <p>
     * This method is called from {@link #deepCopy(Profile)}.
     * <p>
     * Note: If a {@link #profile} is needed for deep copying in the implementation, use {@code clone.profile}.
     * @param clone non-atomic attributes should be deep copied and assigned to the clone.
     */
    protected void deepCopyNonAtomicAttributes(T clone) { }

    /**
     * @return the combined weight of this element and its sub-elements.
     */
    abstract double getWeight();

    /**
     * Check whether the request is allowed, relative to the given position in the profile tree and downwards.
     * @param reasons if the request is not allowed, the reason(s) should be added to {@code reason}.
     * @return true if the request is allowed.
     */
    public boolean isAllowed(List<String> reasons) {
        return true;
    }
}
