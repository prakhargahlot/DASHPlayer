package sg.edu.nus.cs5248.team09.dashplayer.recording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Prakhar on 11-11-2017.
 *
 * Holds the segments that have not yet been uploaded.
 */

public class UploadList extends ArrayList<SegmentationInfo> {

    public static boolean aggressivePush = true;

    private static List<SegmentationInfo> manager;
    public static List<SegmentationInfo> get() {
        if(manager == null) {
            manager = Collections.synchronizedList(new ArrayList<SegmentationInfo>());
        }
        return manager;
    }
}
