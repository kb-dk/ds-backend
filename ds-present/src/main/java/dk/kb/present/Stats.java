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
package dk.kb.present;

import dk.kb.present.model.v1.FormatDto;
import dk.kb.util.Timing;

/**
 * Holds statistics for operations.
 */
public class Stats {
    /**
     * Empty stats for timing parents that defer all measuring to children.
     * <p>
     * Note: Not truly empty as {@link Timing.STATS#name} is still there, but without any other values.
     */
    public static final Timing.STATS[] EMPTY_STATS = new Timing.STATS[]{Timing.STATS.name};

    /**
     * The elements to display when displaying stats. Performance is not affected, only verbosity.
     */
    public static final Timing.STATS[] DEFAULT_STATS = new Timing.STATS[]{
            Timing.STATS.name, Timing.STATS.subject, Timing.STATS.updates, Timing.STATS.ms_updates, Timing.STATS.updates_s,
            Timing.STATS.max_ms, Timing.STATS.last_ms};

    /**
     * Overall statistics for {@link dk.kb.present.api.v1.impl.DsPresentApiServiceImpl#getRecord(String, FormatDto)}.
     * <p>
     * This holds sub-statistics for the different origins & views as well as access.
     */
    public static final Timing GET_RECORD =
            new Timing("getRecord", null, "records", DEFAULT_STATS);

    /**
     * Access resolving statistics for
     * {@link dk.kb.present.api.v1.impl.DsPresentApiServiceImpl#getRecord(String, FormatDto)}.
     * <p>
     * This is a child of {@link #GET_RECORD}.
     */
    public static final Timing RECORD_ACCESS =
            GET_RECORD.getChild("access", null, "checks", DEFAULT_STATS);

    /**
     * Deliver the {@link Timing} responsible for tracting the specified origin with the specified view.
     * @param origin {@link DSOrigin#getId()}.
     * @param view {@link View#getId()}.
     * @return a {@link Timing} for tracking {@link View} processing.
     */
    public static Timing getViewTimer(String origin, String view) {
        return Stats.GET_RECORD.
                getChild("origin_" + origin, null, null, Stats.EMPTY_STATS).
                getChild(view, null, "record", Stats.DEFAULT_STATS);
    }
}
