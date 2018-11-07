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
}
