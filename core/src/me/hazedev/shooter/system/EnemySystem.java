package me.hazedev.shooter.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import me.hazedev.shooter.Mapper;
import me.hazedev.shooter.PolygonFactory;
import me.hazedev.shooter.World;
import me.hazedev.shooter.component.BoundsComponent;
import me.hazedev.shooter.component.BulletComponent;
import me.hazedev.shooter.component.EnemyComponent;
import me.hazedev.shooter.component.HealthComponent;
import me.hazedev.shooter.component.MovementComponent;
import me.hazedev.shooter.component.ParticleEffectComponent;
import me.hazedev.shooter.component.ShooterComponent;
import me.hazedev.shooter.component.SpriteComponent;
import me.hazedev.shooter.component.TransformComponent;
import me.hazedev.shooter.event.listener.CollisionListener;

public class EnemySystem extends IteratingSystem {

    private float timer = 0;
    private int hordeSize = 5;
    private float spawnDelay = 0.2f;
    private float spawnCooldown = 0;
    
    public final World world;

    public EnemySystem(World world) {
        super(Family.all(EnemyComponent.class, MovementComponent.class, TransformComponent.class).get());
        world.signaller.collisionSignal.add(new EnemyCollisionListener());
        this.world = world;
    }

    @Override
    public void update(float delta) {
        super.update(delta);
//        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
//            Vector3 mousePos = world.viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
//            world.spawnEnemy(new Vector2(mousePos.x, mousePos.y));
//        }


//        if (waveDelay > 0) {
//            hordeSize += Math.min(5, 1 + (int) waveDelay);
//        }
//        waveDelay = Math.min(hordeSize + 3, 2 * hordeSize);
        hordeSize = Math.max(5, (int) timer);
        ImmutableArray<Entity> shooters = getEngine().getEntitiesFor(Family.all(TransformComponent.class, ShooterComponent.class).get());
        for (Entity shooter : shooters) {
            TransformComponent shooterTransform = Mapper.TRANSFORM.get(shooter);
            if (getEntities().size() < hordeSize && spawnCooldown == 0) {
                Vector2 offset = new Vector2((world.random.nextFloat() * 0.6f + 0.4f) * world.viewport.getWorldWidth() / 2f, (world.random.nextFloat() * 0.6f + 0.4f) * world.viewport.getWorldHeight() / 2f).scl(world.random.nextBoolean() ? 1 : -1, world.random.nextBoolean() ? 1 : -1);
                Vector2 position = offset.add(shooterTransform.position);
                float random = world.random.nextFloat();
                if (random > 0.25f) {
                    spawnEnemy(position);
                } else if (random > 0.1f) {
                    spawnBlueEnemy(position);
                } else {
                    spawnPurpleEnemy(position);
                }
                spawnCooldown = spawnDelay;
            }
        }
//        waveDelay = Math.max(0, waveDelay - delta);
        spawnCooldown = Math.max(0, spawnCooldown - delta);
        timer += delta;
    }

    @Override
    protected void processEntity(Entity entity, float delta) {
        HealthComponent health = Mapper.HEALTH.get(entity);
        if (health.health > 0) {
            // Scale based on health
            TransformComponent transform = Mapper.TRANSFORM.get(entity);
            transform.scale.set(health.health, health.health);
//            Mapper.SPRITE.get(entity).sprite.setAlpha(health.health/health.maxHealth);

            // Grace period
            EnemyComponent enemy = Mapper.ENEMY.get(entity);
            if (enemy.gracePeriod > 0) enemy.gracePeriod -= delta;

            // Movement
            ImmutableArray<Entity> shooters = getEngine().getEntitiesFor(Family.all(ShooterComponent.class, TransformComponent.class).get());
            Entity closestShooter = null;
            float closestDistance = -1f;
            for (Entity shooter : shooters) {
                TransformComponent shooterTransform = Mapper.TRANSFORM.get(shooter);
                float shooterDistance = transform.position.dst(shooterTransform.position);
                if (closestDistance < 0 || shooterDistance < closestDistance) {
                    closestShooter = shooter;
                    closestDistance = shooterDistance;
                }
            }

            if (closestShooter != null) {
                MovementComponent movement = Mapper.MOVEMENT.get(entity);
                movement.acceleration.add(Mapper.TRANSFORM.get(closestShooter).position.cpy().sub(transform.position)).clamp(0, movement.maxVelocity * 5);
                transform.rotation = MathUtils.radDeg * MathUtils.atan2(movement.velocity.y, movement.velocity.x);
            }

        } else { // DEAD
            ParticleEffectComponent particleEffectComponent = Mapper.PARTICLE_EFFECT.get(entity);
            if (particleEffectComponent == null) {
                world.signaller.enemyDeathSignal.dispatch(entity);
                ParticleEffect effect = world.assets.getEnemyBlast();
                TransformComponent transform = Mapper.TRANSFORM.get(entity);
                Color color = Mapper.SPRITE.get(entity).sprite.getColor();
                effect.getEmitters().forEach(particleEmitter -> {
                    particleEmitter.getTint().setColors(new float[]{color.r, color.g, color.b});
                });
                effect.setPosition(transform.position.x, transform.position.y);
                effect.scaleEffect(transform.scale.x, transform.scale.y, 1);
                effect.start();
                entity.add(new ParticleEffectComponent(effect));
                entity.remove(SpriteComponent.class);
            } else {
                if (particleEffectComponent.effect.isComplete()) {
                    getEngine().removeEntity(entity);
                }
            }
        }
    }

    public class EnemyCollisionListener extends CollisionListener {

        public EnemyCollisionListener() {
            super(Family.all(EnemyComponent.class).get(), Family.all(BulletComponent.class).get());
        }

        @Override
        public void onCollide(Entity enemyEntity, Entity bulletEntity) {
            HealthComponent enemyHealth = Mapper.HEALTH.get(enemyEntity);
            HealthComponent bulletHealth = Mapper.HEALTH.get(bulletEntity);

            if (enemyHealth.health > 0 && bulletHealth.health > 0) {
                EnemyComponent enemy = Mapper.ENEMY.get(enemyEntity);
                BulletComponent bullet = Mapper.BULLET.get(bulletEntity);
                if (enemy.gracePeriod <= 0) {
                    world.assets.getHit().play();
                    bulletHealth.health -= 1;
                    enemyHealth.health -= bullet.damage;
                    enemy.gracePeriod = enemy.damageDelay;
                    if (enemyHealth.health <= 0) {
                        bullet.kills += 1;
                    }
                }
            }

        }

    }

    public void spawnEnemy(Vector2 pos) {
        Entity entity = world.createEntity();

        Polygon arrow = PolygonFactory.getArrow();
        Sprite sprite = new Sprite(world.assets.getArrow());
        sprite.setColor(Color.SCARLET);

        entity.add(new EnemyComponent());
        entity.add(new SpriteComponent(1, sprite));
        entity.add(new TransformComponent(pos));
        entity.add(new MovementComponent(128));
        entity.add(new BoundsComponent(arrow));
        entity.add(new HealthComponent(1));

        world.addEntity(entity);
    }

    public void spawnBlueEnemy(Vector2 pos) {
        Entity entity = world.createEntity();

        Polygon arrow = PolygonFactory.getArrow();
        Sprite sprite = new Sprite(world.assets.getArrow());
        sprite.setColor(new Color(0x0751FFFF));

        entity.add(new EnemyComponent());
        entity.add(new SpriteComponent(1, sprite));
        entity.add(new TransformComponent(pos));
        entity.add(new MovementComponent(92));
        entity.add(new BoundsComponent(arrow));
        entity.add(new HealthComponent(3));

        world.addEntity(entity);
    }

    public void spawnPurpleEnemy(Vector2 pos) {
        Entity entity = world.createEntity();

        Polygon arrow = PolygonFactory.getArrow();
        Sprite sprite = new Sprite(world.assets.getArrow());
        sprite.setColor(Color.PURPLE);

        entity.add(new EnemyComponent());
        entity.add(new SpriteComponent(1, sprite));
        entity.add(new TransformComponent(pos));
        entity.add(new MovementComponent(256));
        entity.add(new BoundsComponent(arrow));
        entity.add(new HealthComponent(1));

        world.addEntity(entity);
    }

}
