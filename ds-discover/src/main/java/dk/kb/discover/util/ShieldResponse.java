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
package dk.kb.discover.util;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates whether the request is allowed, along with a reason when it is not and
 * the weight of the request.
 */
public class ShieldResponse {
    public final Map<String, List<String>> request;
    public final double maxWeight;
    public final boolean allowed;
    // TODO: Add warnings (large fields, high numbers) to the response
    public final String reason;
    public final double weight;

    /**
     * @param request   the unmodified originating request.
     * @param maxWeight the maximum allowed weight.
     * @param allowed   true if the {@code request} is allowed, else false.
     * @param reason    if {@code allowed == false}, this will hold a human readable reason as to why the
     *                  {@code request} is not allowed.
     * @param weight    the weight of the {@code request}.
     */
    public ShieldResponse(Map<String, List<String>> request, double maxWeight,
                          boolean allowed, String reason, double weight) {
        this.request = request;
        this.maxWeight = maxWeight;
        this.allowed = allowed;
        this.reason = reason;
        this.weight = weight;
    }

    /**
     * @param maxWeight the maxWeight of the {@link #request}.
     * @return a copy of this ShieldResponse adjusted with the given value.
     */
    public ShieldResponse maxWeight(double maxWeight) {
        return new ShieldResponse(request, maxWeight, allowed, reason, weight);
    }

    /**
     * @param allowed whether the request is allowed to be issued to a Solr installation.
     * @return a copy of this ShieldResponse adjusted with the given value.
     */
    public ShieldResponse allowed(boolean allowed) {
        return new ShieldResponse(request, maxWeight, allowed, reason, weight);
    }

    /**
     * @param reason the reason why the {@link #request} is not allowed.
     * @return a copy of this ShieldResponse adjusted with the given value.
     */
    public ShieldResponse reason(String reason) {
        return new ShieldResponse(request, maxWeight, allowed, reason, weight);
    }

    /**
     * @param weight the weight of the {@link #request}.
     * @return a copy of this ShieldResponse adjusted with the given value.
     */
    public ShieldResponse reason(double weight) {
        return new ShieldResponse(request, maxWeight, allowed, reason, weight);
    }
}
