package com.stickercamera.app.model;

import com.alibaba.fastjson.JSON;
import com.common.util.StringUtils;

import java.util.List;


/**
 * Local simple use, the actual project with the sticker-related attributes can be added to this category
 */
public class Addon  {
    private int    id;

    //JSON用到
    public Addon() {

    }

    public Addon(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
