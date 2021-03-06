package me.hazedev.shooter.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class MovementComponent implements Component {

    public final Vector2 velocity;
    public final Vector2 acceleration;
    public float minVelocity = 0;
    public float maxVelocity;

    public MovementComponent(Vector2 velocity, Vector2 acceleration, float minVelocity, float maxVelocity) {
        this.velocity = velocity;
        this.acceleration = acceleration;
        this.minVelocity = minVelocity;
        this.maxVelocity = maxVelocity;
    }

    public MovementComponent(Vector2 velocity, Vector2 acceleration, float maxVelocity) {
        this.velocity = velocity;
        this.acceleration = acceleration;
        this.maxVelocity = maxVelocity;
    }

    public MovementComponent(Vector2 velocity, Vector2 acceleration) {
        this.velocity = velocity;
        this.acceleration = acceleration;
        this.maxVelocity = velocity.len();
    }

    public MovementComponent(Vector2 velocity) {
        this(velocity, new Vector2());
    }

    public MovementComponent(float maxVelocity) {
        this.velocity = new Vector2();
        this.acceleration = new Vector2();
        this.maxVelocity = maxVelocity;
    }

    public MovementComponent() {
        this(new Vector2(), new Vector2());
    }

}
