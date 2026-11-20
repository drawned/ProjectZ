package me.drawn.projectz.inventory;

import net.minecraft.nbt.CompoundTag;

public class WeaponBarState {
    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;

    private int selected = PRIMARY;

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected == SECONDARY ? SECONDARY : PRIMARY;
    }

    public void toggle() {
        selected = selected == PRIMARY ? SECONDARY : PRIMARY;
    }

    public void save(CompoundTag tag) {
        tag.putInt("Selected", selected);
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            selected = PRIMARY;
            return;
        }
        selected = tag.getInt("Selected") == SECONDARY ? SECONDARY : PRIMARY;
    }
}
