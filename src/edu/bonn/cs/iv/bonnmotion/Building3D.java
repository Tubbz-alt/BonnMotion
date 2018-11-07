package edu.bonn.cs.iv.bonnmotion;

public class Building3D extends Building {
	public Building3D(double x1, double x2, double y1, double y2, double z1, double z2, double doorx, double doory, double doorz) {
		super(x1, x2, y1, y2, doorx, doory);

		this.z1 = z1;
		this.z2 = z2;
		this.doorz = doorz;
	}

	protected double z1 = 0;
	protected double z2 = 0;
	protected double doorz = 0;

	/**
	 * Checks if the given position is inside the building
	 * @param pos
	 * @return true if the position is inside the building, false otherwise
	 */
	public boolean isInside(Position3D pos) {
		if (!(pos instanceof Position3D)) {
			throw new IllegalArgumentException("Building3D can only be used with 3D Positions");
		}
		
		if(pos.x > this.x1 && pos.x < this.x2 
				&& pos.y > this.y1 && pos.y < this.y2
				&& pos.z > this.z1 && pos.z < this.z2)
			return true;
		return false;
	}

	public boolean canCommunicateThroughDoor(Position3D _pos1, Position3D _pos2) {
		if (!(_pos1 instanceof Position3D) || !(_pos2 instanceof Position3D)) {
			throw new IllegalArgumentException("Building3D can only be used with 3D Positions");
		}
		
		Position3D pos1 = (Position3D) _pos1;
		Position3D pos2 = (Position3D) _pos2;
		
		if (this.x1 == this.doorx) {
			// door is on the left
			if (pos2.x > this.doorx - 10
					&& pos2.y == this.doory
					&& pos1.x < this.doorx + 10
					&& pos1.y == this.doory
					&& pos1.z == this.doorz
					&& pos2.z == this.doorz) {
				return true;
			}
		} else if (this.x2 == this.doorx) {
			// door is on the right
			if (pos2.x < this.doorx + 10
					&& pos2.y == this.doory
					&& pos1.x > this.doorx - 10
					&& pos1.y == this.doory
					&& pos1.z == this.doorz
					&& pos2.z == this.doorz) {
				return true;
			}
		} else if (this.y1 == this.doory) {
			// door is on the south
			if (pos2.y > this.doory - 10
					&& pos2.x == this.doorx
					&& pos1.x == this.doorx
					&& pos1.y < this.doory + 10
					&& pos1.z == this.doorz
					&& pos2.z == this.doorz) {
				return true;
			}
		} else if (this.y2 == this.doory) {
			// door is on the north
			if (pos2.y < this.doory + 10
					&& pos2.x == this.doorx
					&& pos1.x == this.doorx
					&& pos1.y > this.doory - 10
					&& pos1.z == this.doorz
					&& pos2.z == this.doorz) {
				return true;
			}
		} else if (this.z1 == this.doorz) {
			// door is on the bottom
			if (pos1.x == this.doorx
					&& pos2.x == this.doorx
					&& pos1.y == this.doory
					&& pos2.y == this.doory
					&& pos1.z < this.doorz + 10
					&& pos2.z > this.doorz - 10) {
				return true;
			}
		} else if (this.z2 == this.doorz) {
			// door is on the top
			if (pos1.x == this.doorx
					&& pos2.x == this.doorx
					&& pos1.y == this.doory
					&& pos2.y == this.doory
					&& pos1.z > this.doorz - 10
					&& pos2.z < this.doorz + 10) {
				return true;
			}
		}
		
		return false;
	}
}
