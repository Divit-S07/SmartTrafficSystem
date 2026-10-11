package com.vegam.traffic.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A junction (node) of the road network.
 *
 * <p>The business id (e.g. "J1") is used directly as the MongoDB {@code _id},
 * so junction ids are inherently unique. The {@code x}/{@code y} coordinates
 * preserve the map layout used by the React frontend.
 */
@Document(collection = "junctions")
public class Junction {

    @Id
    @NotBlank(message = "Junction id must not be blank")
    private String id;

    @NotNull(message = "Junction x coordinate must not be null")
    private Double x;

    @NotNull(message = "Junction y coordinate must not be null")
    private Double y;

    public Junction() {
    }

    public Junction(String id, Double x, Double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }
}
