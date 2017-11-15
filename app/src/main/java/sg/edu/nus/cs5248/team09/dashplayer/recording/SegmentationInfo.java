package sg.edu.nus.cs5248.team09.dashplayer.recording;

import sg.edu.nus.cs5248.team09.dashplayer.CommonUtilities;

/**
 * Created by Prakhar on 11-11-2017.
 *
 * Encapsulates all information about a single video's segmentation
 */

public class SegmentationInfo {
    private String originalVideoName;
    private String uploadName;
    private int segments;

    SegmentationInfo(String originalVideoName, int segments) {
        this.originalVideoName = originalVideoName;
        this.segments = segments;
    }

    private int getSegments() {
        return segments;
    }

    int size() {
        return getSegments();
    }

    String getOriginalVideoName() {
        return originalVideoName;
    }

    String getUploadName() {
        return uploadName;
    }

    void setUploadName(String uploadName) {
        this.uploadName = uploadName;
    }}
