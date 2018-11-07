package edu.bonn.cs.iv.bonnmotion;

public class Building {
	public Building(double x1, double x2, double y1, double y2, double doorx, double doory) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.doorx = doorx;
		this.doory = doory;
	}

	protected double x1 = 0;
	protected double x2 = 0;
	protected double y1 = 0;
	protected double y2 = 0;
	protected double doorx = 0;
	protected double doory = 0;

	/**
	 * Checks if the given position is inside the building
	 * @param pos
	 * @return true if the position is inside the building, false otherwise
	 */
	public boolean isInside(Position pos) {
		if (pos.x > this.x1 && pos.x < this.x2 && pos.y > this.y1 && pos.y < this.y2)
			return true;
		else
			return false;
	}

	/**
	 * Checks if the given positions can communicate through a door, e.g.
	 *    __________
	 *    |        |
	 *    |      x   x
	 *    |________|    
	 * @param pos1 The position inside the building
	 * @param pos2 The position outside the building
	 * @return true if the positions can communicate with each other, false otherwise
	 */
	public boolean canCommunicateThroughDoor(Position pos1, Position pos2) {
		if (this.x1 == this.doorx) {
			// door is on the left
			if (pos2.x > this.doorx - 10
					&& pos2.y == this.doory
					&& pos1.x < this.doorx + 10
					&& pos1.y == this.doory) {
				return true;
			}
		} else if (this.x2 == this.doorx) {
			// door is on the right
			if (pos2.x < this.doorx + 10
					&& pos2.y == this.doory
					&& pos1.x > this.doorx - 10
					&& pos1.y == this.doory) {
				return true;
			}
		} else if (this.y1 == this.doory) {
			// door is on the south
			if (pos2.y > this.doory - 10
					&& pos2.x == this.doorx
					&& pos1.x == this.doorx
					&& pos1.y < this.doory + 10) {
				return true;
			}
		} else if (this.y2 == this.doory) {
			// door is on the north
			if (pos2.y < this.doory + 10
					&& pos2.x == this.doorx
					&& pos1.x == this.doorx
					&& pos1.y > this.doory - 10) {
				return true;
			}
		}
		return false;
	}
}
