package com.nonumberstudios.mob_randomizer.mixin;

import com.nonumberstudios.mob_randomizer.MobRandomizerMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin( ServerWorld.class )
public class ServerWorldMixin {

	@ModifyVariable( method = "spawnEntity", at = @At( "HEAD" ), argsOnly = true )
	public Entity spawnEntity( Entity entity ) {
		ServerWorldAccess world = (ServerWorldAccess)(Object)this;

		if ( entity == null || !MobRandomizerMod.canRandomize( entity.getType() ) ) return entity;

		EntityType<?> newType = MobRandomizerMod.randomize( entity.getType() );

		//MobRandomizerMod.LOGGER.info( "Randomized {} -> {}", entity.getType(), newType );

		Entity newEntity = newType.create( world.toServerWorld() );
		if ( newEntity == null ) return entity;

		newEntity.copyPositionAndRotation( entity );
		if ( entity instanceof MobEntity &&  newEntity instanceof MobEntity && ( ( MobEntity ) entity ).isPersistent() ) {
			( ( MobEntity ) newEntity ).setPersistent();
		}

		if ( entity.hasVehicle() ) {
			newEntity.startRiding( entity.getVehicle(), true );
		}

		return newEntity;
	}
}
