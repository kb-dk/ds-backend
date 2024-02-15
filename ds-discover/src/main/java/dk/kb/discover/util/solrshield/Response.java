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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Encapsulates whether the request is allowed, along with a reason when it is not and
 * the weight of the request.
 */
public class Response {
    public final Iterable<Map.Entry<String, String[]>> request;
    public final double maxWeight;
    public final boolean allowed;
    // TODO: Add warnings (large fields, high numbers) to the response
    public final Collection<String> reasons;
    public final double weight;

    /**
     * @param request   the unmodified originating request.
     * @param maxWeight the maximum allowed weight.
     * @param allowed   true if the {@code request} is allowed, else false.
     * @param reasons   if {@code allowed == false}, this will hold human readable reasons as to why the
     *                  {@code request} is not allowed.
     * @param weight    the weight of the {@code request}.
     */
    public Response(Iterable<Map.Entry<String, String[]>> request, double maxWeight,
                    boolean allowed, Collection<String> reasons, double weight) {
        this.request = request;
        this.maxWeight = maxWeight;
        this.allowed = allowed;
        this.reasons = reasons == null ? Collections.emptyList() : reasons;
        this.weight = weight;
    }

    /**
     * @param maxWeight the maxWeight of the {@link #request}.
     * @return a copy of this Response adjusted with the given value.
     */
    public Response maxWeight(double maxWeight) {
        return new Response(request, maxWeight, allowed, reasons, weight);
    }

    /**
     * @param allowed whether the request is allowed to be issued to a Solr installation.
     * @return a copy of this Response adjusted with the given value.
     */
    public Response allowed(boolean allowed) {
        return new Response(request, maxWeight, allowed, reasons, weight);
    }

    /**
     * @param reasons the reasons why the {@link #request} is not allowed.
     * @return a copy of this Response adjusted with the given value.
     */
    public Response reasons(Collection<String> reasons) {
        return new Response(request, maxWeight, allowed, reasons, weight);
    }

    /**
     * Add a single reason to the collection of reasons.
     * @param reason a reasons why the {@link #request} is not allowed.
     * @return a copy of this Response adjusted with the given value.
     */
    public Response addReason(String reason) {
        List<String> newReasons = new ArrayList<>(reasons);
        newReasons.add(reason);
        return new Response(request, maxWeight, allowed, newReasons, weight);
    }


    /**
     * @param weight the weight of the {@link #request}.
     * @return a copy of this Response adjusted with the given value.
     */
    public Response weight(double weight) {
        return new Response(request, maxWeight, allowed, reasons, weight);
    }

    @Override
    public String toString() {
        return "Response{" +
                "request=" + toString(request) +
                ", maxWeight=" + maxWeight +
                ", allowed=" + allowed +
                ", weight=" + weight +
                ", reasons=" + reasons +
                '}';
    }

    private String toString(Iterable<Map.Entry<String, String[]>> request) {
        return StreamSupport.stream(request.spliterator(), false)
                .map(e -> e.getKey() + "=" + Arrays.toString(e.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

}
