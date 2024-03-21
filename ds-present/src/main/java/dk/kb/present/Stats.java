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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds statistics for operations.
 */
public class Stats {
    private static final Logger log = LoggerFactory.getLogger(Stats.class);

    /**
     * Empty stats for timing parents that defer all measuring to children.
     */
    public static final Timing.STATS[] EMPTY_STATS = new Timing.STATS[]{};

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

}
