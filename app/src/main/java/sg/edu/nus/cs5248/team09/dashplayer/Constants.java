package sg.edu.nus.cs5248.team09.dashplayer;

import android.os.Environment;

/**
 * Created by Prakhar
 *
 * Constants needed in the app
 */

@SuppressWarnings("WeakerAccess")
public class Constants {
    public static final String FILE_MPD = "/play.mpd";
    public static final String LOCAL_HOME = Environment.getExternalStorageDirectory() + "/DASHPlayer/";
    public static final String RECORD_VIDEO_FOLDER = LOCAL_HOME + "Uploads/";
    public static final String SEGMENT_CREATE_FOLDER = LOCAL_HOME + "Uploads/segments/";
    public static final String UPLOAD_VID_EXTENSION = ".mp4";
    public static final String JOINER = "_";

    public class Server {
        public static final String URL = "http://monterosa.d2.comp.nus.edu.sg/~team09/content/";
        public static final String UPLOAD_URL = URL + "upload.php";
        public static final String BASE_URL = URL + "uploads/";
        public static final String VIDEO_LIST_URL = URL + "videos.php";
        public static final String LAST_SEGMENT_URL = URL + "last.php";
        public static final String MPD_URL = URL + "slay.php";
    }

    public class FormAttributes {
        public static final String TYPE = "type";
        public static final String VID_NAME = "video_name";
        public static final String SEG_NO = "sequence_number";
        public static final String FILE_TO_UPLOAD = "fileToUpload";
        public static final String LAST_SEGMENT = "lastSegment";
    }

    public class Resolutions {

        public class Dimensions {
            public static final int LOW = 426;
            public static final int MID = 640;
            public static final int HIGH = 854;
        }

        public static final byte NONE = -1;
        public static final byte LOW = 0;
        public static final byte MID = 1;
        public static final byte HIGH = 2;
    }

    public class XMLNames {

        public class Tags {
            public static final String REPRESENTATION = "Representation";
            public static final String SEGMENT_URL = "SegmentURL";
        }

        public class Attributes {
            public static final String MEDIA = "media";
        }
    }
}
