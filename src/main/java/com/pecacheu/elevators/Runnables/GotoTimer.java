package com.pecacheu.elevators.Runnables;

import com.pecacheu.elevators.Conf;
import com.pecacheu.elevators.Entities.Elevator;
import com.pecacheu.elevators.Main;
import org.bukkit.scheduler.BukkitRunnable;

public class GotoTimer extends BukkitRunnable {
    private Main plugin = Conf.plugin; private Elevator elev;
    private int sLevel, selNum, fID; private double fPos, step, accel;

    public void set(Elevator elev, int fLevel, int sLevel, int selNum, double speed, double step, int fID) {
        this.elev = elev; this.step = step; this.fPos = fLevel; this.sLevel = sLevel; this.selNum = selNum;
        this.fID = fID; this.accel = speed*(elev.moveDir?1:-1)/21.2; elev.setEntities(false, fPos, false);
    }

    public void run() { synchronized(Conf.API_SYNC) {
        elev.floor.moveFloor(fID, fPos); elev.updateCallSigns(fPos, elev.moveDir?1:0, selNum);
        elev.setEntities(false, accel, fPos, false);
        if(elev.moveDir?(fPos >= sLevel):(fPos <= sLevel)) { //At destination floor:
            this.cancel(); elev.floor.deleteFloor(fID); plugin.setTimeout(() -> {
                elev.floor.addFloor(sLevel, false); elev.setEntities(true, sLevel, true); //Restore solid floor.
                elev.updateCallSigns(sLevel+2); plugin.setTimeout(() -> {
                    elev.floor.moving = false; elev.updateCallSigns(sLevel+2); elev.doorTimer(sLevel+2);
                }, 500);
            }, 50);
        } else fPos += step;
    }}
}