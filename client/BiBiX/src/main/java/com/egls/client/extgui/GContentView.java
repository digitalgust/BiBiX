/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.egls.client.extgui;

import com.egls.client.BbMain;
import org.mini.gui.GObject;
import org.mini.gui.GViewPort;

/**
 *
 * @author Gust
 */
public class GContentView extends GViewPort {

    public static float itemW, itemH;
    public static final float PAD = 10;

    BbMain app;

    public GContentView(BbMain app) {
        this.app = app;
    }

    public float getAfterLastItem() {
        float h = 0;
        int size = getElements().size();
        if (size > 0) {
            GContentItem ci = (GContentItem) getElements().get(size - 1);
            h = ci.getLocationTop() + ci.getH();
        }
        return h;
    }

    public GContentItem findSessionItem(long friendid, long sessionid) {
        for (GObject go : getElements()) {
            GContentItem gsi = (GContentItem) go;
            if (gsi.msg.fromid == friendid && gsi.msg.groupid == sessionid) {
                return gsi;
            }
        }
        return null;
    }

    public void addItem(GContentItem in) {

        int size = getElementSize();

        if (size > 0) {
            GContentItem last = (GContentItem) getElements().get(size - 1);

            if (last.msg.time < in.msg.time) {
                add(in);
            } else {
                boolean move = false;
                float y = 0;
                for (int i = 0; i < size; i++) {
                    GContentItem gci = (GContentItem) getElements().get(i);
                    if (!move) {
                        long dif = gci.msg.time - in.msg.time;
                        if (dif > 0) {
                            add(i, in);
                            size++;
                            in.setLocation(in.getLocationLeft(), gci.getLocationTop());
                            move = true;
                            y = in.getLocationTop() + in.getH();

                        }
                    } else {
                        gci.setLocation(gci.getLocationLeft(), y);
                        y += gci.getH();
                    }
                }
            }
        } else {
            add(in);
        }

        reBoundle();
        setScrollY((in.getLocationTop() + in.getH()) / getInnerH());
    }
    //
    //    public void removeItem(GSessionItem gsi) {
    //        boolean found = false;
    //        int i = 0;
    //        for (Iterator<GObject> it = getElements().iterator(); it.hasNext();) {
    //            GObject go = it.next();
    //            if (go.equals(gsi)) {
    //                it.remove();
    //                found = true;
    //                i--;
    //            }
    //            i++;
    //            if (found) {
    //                go.setLocation(PAD, i * itemH);
    //            }
    //        }
    //    }
    //
    //    void reAlign() {
    //        int i = 0;
    //        for (GObject go : getElements()) {
    //            go.setLocation(PAD, i * itemH);
    //            i++;
    //        }
    //    }
    //
    //    public void removeItem(GSessionItem gsi) {
    //        boolean found = false;
    //        int i = 0;
    //        for (Iterator<GObject> it = getElements().iterator(); it.hasNext();) {
    //            GObject go = it.next();
    //            if (go.equals(gsi)) {
    //                it.remove();
    //                found = true;
    //                i--;
    //            }
    //            i++;
    //            if (found) {
    //                go.setLocation(PAD, i * itemH);
    //            }
    //        }
    //    }

    public boolean update(long vg) {
        //GToolkit.drawRect(vg, getViewX() + 1, getViewY() + 1, getViewW() - 2, getViewH() - 2, GToolkit.getStyle().getEditBackground());
        super.update(vg);
        return true;
    }

    @Override
    public boolean scrollEvent(float scrollX, float scrollY, float x, float y) {
        return dragEvent(scrollX, scrollY, x, y);
    }

    @Override
    public boolean dragEvent(float dx, float dy, float x, float y) {
        float scrollY = getScrollY();
        if ((scrollY == 0 || (getH() - getInnerH() == 0)) && dy > 0) {
            app.loadPrePage();
        } else if ((scrollY == 1 || (getH() - getInnerH() == 0)) && dy < 0) {
            app.loadNextPage();
        }
        reBoundle();
        flush();
        return super.dragEvent(dx, dy, x, y);
    }

}
