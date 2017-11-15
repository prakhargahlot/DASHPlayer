package sg.edu.nus.cs5248.team09.dashplayer.playback;

import android.support.annotation.NonNull;

/**
 * Author: Prakhar
 *
 * Class to encapsulate Downloaded Videos
 */
public class Video implements Comparable{

    private String name;
    private int id;

    Video(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        Video that = (Video) o;
        return this.name.compareTo(that.name);
    }
}
