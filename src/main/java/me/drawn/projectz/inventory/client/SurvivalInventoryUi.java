package me.drawn.projectz.inventory.client;

/**
 * Tema escuro minimalista. Coordenadas relativas ao painel, compartilhadas pelo menu e pela tela.
 * Esquerda: personagem, armadura e as duas mãos. Direita: mochila 7×7.
 */
public final class SurvivalInventoryUi {
    public static final int PANEL_WIDTH = 420;
    public static final int PANEL_HEIGHT = 252;
    public static final int PAD = 14;

    public static final int TAB_HEIGHT = 22;
    public static final int TAB_WIDTH = 96;
    public static final int TAB_GAP = 6;
    public static final int TAB_STRIP_Y = -TAB_HEIGHT - 1;

    public static final int CELL = 20;
    public static final int SLOT = 18;

    public static final int LEFT_X = 16;
    public static final int CONTENT_Y = 16;

    public static final int ARMOR_X = 18;
    public static final int ARMOR_Y = 36;

    public static final int MODEL_X = 48;
    public static final int MODEL_Y = 36;
    public static final int MODEL_W = 78;
    public static final int MODEL_H = 92;

    public static final int WEAPON_PRIMARY_SIZE = 40;
    public static final int WEAPON_SECONDARY_SIZE = 26;
    public static final int WEAPON_PRIMARY_X = 18;
    public static final int WEAPON_PRIMARY_Y = 176;
    public static final int WEAPON_SECONDARY_X = 66;
    public static final int WEAPON_SECONDARY_Y = 186;
    public static final int OFFHAND_X = 100;
    public static final int OFFHAND_Y = 190;

    public static final int GRID_X = 196;
    public static final int GRID_Y = 36;

    public static final int CRAFT_GRID_X = 48;
    public static final int CRAFT_GRID_Y = 72;
    public static final int CRAFT_RESULT_X = 140;
    public static final int CRAFT_RESULT_Y = 92;
    public static final int CRAFT_ARROW_X = 118;
    public static final int CRAFT_ARROW_Y = 98;

    public static final int COLOR_SCRIM = 0xC0060606;
    public static final int COLOR_PANEL = 0xF2111111;
    public static final int COLOR_PANEL_EDGE = 0xFF2C2C2C;
    public static final int COLOR_PANEL_INSET = 0xFF0E0E0E;
    public static final int COLOR_SLOT = 0xFF1A1A1A;
    public static final int COLOR_SLOT_BORDER = 0xFF343434;
    public static final int COLOR_SLOT_HOVER = 0x55FFFFFF;
    public static final int COLOR_SLOT_ACTIVE = 0xFF2A2A2A;
    public static final int COLOR_ACCENT = 0xFFE8E8E8;
    public static final int COLOR_ACCENT_DIM = 0xFF6E6E6E;
    public static final int COLOR_BLOCKED = 0xFF141414;
    public static final int COLOR_ITEM_BACK = 0xE01E1E1E;
    public static final int COLOR_TAB_BACK = 0xFF161616;
    public static final int COLOR_TAB_ACTIVE = 0xFF222222;
    public static final int COLOR_TEXT = 0xFFF0F0F0;
    public static final int COLOR_TEXT_DIM = 0xFF8A8A8A;
    public static final int COLOR_DANGER = 0xFF8C4E4E;

    private SurvivalInventoryUi() {}
}
