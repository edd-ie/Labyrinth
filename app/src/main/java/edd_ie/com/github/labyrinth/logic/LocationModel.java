package edd_ie.com.github.labyrinth.logic;

public class LocationModel {

    private double startLocation;
    private double endLocation;
    private String room;
    private String building;

    public LocationModel() {
    }

    public LocationModel(float start, float end, String roomData, String buildingData) {
        startLocation = start;
        endLocation = end;
        room = roomData;
        building = buildingData;
    }


    public double getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(double startLocation) {
        this.startLocation = startLocation;
    }

    public double getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(double endLocation) {
        this.endLocation = endLocation;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }
}
