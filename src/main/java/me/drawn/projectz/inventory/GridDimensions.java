package me.drawn.projectz.inventory;

public record GridDimensions(int width, int height) {
    public static final GridDimensions ONE = new GridDimensions(1, 1);

    public GridDimensions {
        width = Math.max(1, width);
        height = Math.max(1, height);
    }

    public int area() {
        return width * height;
    }

    public boolean covers(int originX, int originY, int cellX, int cellY) {
        return cellX >= originX && cellX < originX + width && cellY >= originY && cellY < originY + height;
    }

    public boolean fits(int x, int y, int columns, int rows) {
        return x >= 0 && y >= 0 && x + width <= columns && y + height <= rows;
    }
}
