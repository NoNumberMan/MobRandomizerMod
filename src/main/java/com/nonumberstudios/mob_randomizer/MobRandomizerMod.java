package com.nonumberstudios.mob_randomizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class MobRandomizerMod implements ModInitializer {
	//TODO make excluded list expandable
	//TODO wandering trader, mobs that spawn with structures such as bastions

	public static final String MODID = "mob_randomizer";
	public static final Logger LOGGER = LoggerFactory.getLogger( MODID );
	private static final EntityType<?>[] excluded = new EntityType<?>[]{
			EntityType.ZOMBIE_HORSE, EntityType.GIANT, EntityType.ILLUSIONER, EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.ELDER_GUARDIAN
	};

	private static final EntityType<?>[] defaultBlazeMappings = new EntityType<?>[] {
			EntityType.AXOLOTL, EntityType.BAT, EntityType.CAT,  EntityType.CHICKEN, EntityType.COD,
			EntityType.COW, EntityType.DONKEY, EntityType.FOX, EntityType.GLOW_SQUID, EntityType.HORSE,
			EntityType.MOOSHROOM, EntityType.MULE, EntityType.OCELOT, EntityType.PARROT, EntityType.PIG,
			EntityType.PUFFERFISH, EntityType.RABBIT, EntityType.SALMON, EntityType.SHEEP, EntityType.SQUID,
			EntityType.STRIDER, EntityType.TROPICAL_FISH, EntityType.TURTLE, EntityType.VILLAGER,
			EntityType.BEE, EntityType.CAVE_SPIDER, EntityType.DOLPHIN, EntityType.ENDERMAN, EntityType.LLAMA,
			EntityType.PANDA, EntityType.PIGLIN, EntityType.POLAR_BEAR, EntityType.SPIDER, EntityType.WOLF,
			EntityType.ZOMBIFIED_PIGLIN, EntityType.BLAZE, EntityType.CREEPER, EntityType.DROWNED, EntityType.GHAST,
			EntityType.GUARDIAN, EntityType.HOGLIN, EntityType.HUSK, EntityType.MAGMA_CUBE, EntityType.PHANTOM,
			EntityType.PILLAGER, EntityType.RAVAGER, EntityType.SKELETON, EntityType.SLIME, EntityType.STRAY,
			EntityType.WITCH, EntityType.WITHER_SKELETON, EntityType.ZOGLIN, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER
	};

	private static final EntityType<?>[] defaultEndermanMappings = new EntityType<?>[]{
			EntityType.AXOLOTL, EntityType.BAT, EntityType.CAT,  EntityType.CHICKEN, EntityType.COD,
			EntityType.COW, EntityType.DONKEY, EntityType.FOX, EntityType.GLOW_SQUID, EntityType.HORSE,
			EntityType.MOOSHROOM, EntityType.MULE, EntityType.OCELOT, EntityType.PARROT, EntityType.PIG,
			EntityType.PUFFERFISH, EntityType.RABBIT, EntityType.SALMON, EntityType.SHEEP, EntityType.SQUID,
			EntityType.STRIDER, EntityType.TROPICAL_FISH, EntityType.TURTLE, EntityType.VILLAGER,
			EntityType.BEE, EntityType.CAVE_SPIDER, EntityType.DOLPHIN, EntityType.ENDERMAN, EntityType.LLAMA,
			EntityType.PANDA, EntityType.PIGLIN, EntityType.POLAR_BEAR, EntityType.SPIDER, EntityType.WOLF,
			EntityType.ZOMBIFIED_PIGLIN, EntityType.BLAZE, EntityType.CREEPER, EntityType.DROWNED, EntityType.GHAST,
			EntityType.GUARDIAN, EntityType.HOGLIN, EntityType.HUSK, EntityType.MAGMA_CUBE, EntityType.PHANTOM,
			EntityType.PILLAGER, EntityType.RAVAGER, EntityType.SKELETON, EntityType.SLIME, EntityType.STRAY,
			EntityType.WITCH, EntityType.WITHER_SKELETON, EntityType.ZOGLIN, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER
	};

	private static final Map<Integer, ArrayList<Integer>> allowedMappings = new HashMap<>();
	private static final Map<Integer, Integer> randomizer = new HashMap<>();
	private static final Map<Integer, Integer> compliment = new HashMap<>();


	public static boolean canRandomize( EntityType<?> entity ) {
		return entity.getSpawnGroup() != SpawnGroup.MISC && Arrays.stream( excluded ).noneMatch( e -> e == entity );
	}

	@NotNull
	public static EntityType<?> randomize( EntityType<?> entityIn ) {
		if ( !canRandomize( entityIn ) ) {
			return entityIn;
		}

		int id = Registry.ENTITY_TYPE.getRawId( entityIn );
		return Registry.ENTITY_TYPE.get( randomizer.get( id ) );
	}

	@NotNull
	public static EntityType<?> compliment( EntityType<?> entityIn ) {
		if ( !canRandomize( entityIn ) ) {
			return entityIn;
		}

		int id = Registry.ENTITY_TYPE.getRawId( entityIn );
		return Registry.ENTITY_TYPE.get( compliment.get( id ) );
	}

	private static void readConfig( Map<Integer, ArrayList<Integer>> map ) {
		String configPath = FabricLoader.getInstance().getConfigDir() + File.separator + MODID + ".json";
		File configFile = new File( configPath );
		if ( configFile.exists() ) {
			try {
				FileReader fileReader = new FileReader( configFile );
				String content = Files.readString( Paths.get( configPath ) );
				JsonObject config = JsonHelper.deserialize( content, true );
				JsonArray allowedMappings = config.getAsJsonArray( "allowedMappings" );

				allowedMappings.forEach( mapElement -> {
					JsonObject entry = mapElement.getAsJsonObject();
					Identifier id = Identifier.tryParse( entry.getAsJsonPrimitive( "mob-id" ).getAsString() );
					if ( Registry.ENTITY_TYPE.containsId( id ) ) {
						int rawId = Registry.ENTITY_TYPE.getRawId( Registry.ENTITY_TYPE.get( id ) );
						ArrayList<Integer> list = new ArrayList<>();
						JsonArray mappings = entry.getAsJsonArray( "mappings" );
						mappings.forEach( idElement -> {
							Identifier mobid = Identifier.tryParse( idElement.getAsString() );
							list.add( Registry.ENTITY_TYPE.getRawId( Registry.ENTITY_TYPE.get( mobid ) ) );
						} );
						map.put( rawId, list );
					} else {
						LOGGER.error( "Loaded invalid mob id: {}", id );
					}
				} );

				return;
			} catch ( Exception exception ) {
				LOGGER.error( "Failed to read config file with exception {}", exception.getMessage() );
			}
		}

		LOGGER.info( "Creating new config file..." );

		try {
			JsonArray blazeMappings = new JsonArray();
			JsonArray endermanMappings = new JsonArray();

			for ( EntityType<?> entity : defaultBlazeMappings ) {
				blazeMappings.add( Registry.ENTITY_TYPE.getId( entity ).toString() );
			}

			for ( EntityType<?> entity : defaultEndermanMappings ) {
				endermanMappings.add( Registry.ENTITY_TYPE.getId( entity ).toString() );
			}

			JsonObject blazeEntry = new JsonObject();
			blazeEntry.addProperty( "mob-id", Registry.ENTITY_TYPE.getId( EntityType.BLAZE ).toString() );
			blazeEntry.add( "mappings", blazeMappings );

			JsonObject endermanEntry = new JsonObject();
			endermanEntry.addProperty( "mob-id", Registry.ENTITY_TYPE.getId( EntityType.ENDERMAN ).toString() );
			endermanEntry.add( "mappings", endermanMappings );

			JsonArray mappingsArray = new JsonArray();
			mappingsArray.add( blazeEntry );
			mappingsArray.add( endermanEntry );

			JsonObject config = new JsonObject();
			config.add( "allowedMappings", mappingsArray );

			Files.writeString( Paths.get( configPath ), config.toString(), StandardOpenOption.WRITE, StandardOpenOption.CREATE );
		} catch ( Exception exception ) {
			LOGGER.error( "Failed to create config file with exception {}", exception.toString() );
			LOGGER.error( "Proceeding without config..." );
		}

		LOGGER.info( "Created new config successfully!" );
	}


	@Override
	public void onInitialize() {
		readConfig( allowedMappings );

		ArrayList<Integer> ids = new ArrayList<>();
		ArrayList<Integer> available = new ArrayList<>();

		Registry.ENTITY_TYPE.forEach( ( EntityType<?> entity ) -> {
			if ( canRandomize( entity ) ) {
				int id = Registry.ENTITY_TYPE.getRawId( entity );
				ids.add( id );
				available.add( id );
			}
		} );

		Random random = new Random();

		//prioritize configured mappings
		for ( int id : allowedMappings.keySet() ) {
			if ( ids.contains( id ) ) {
				ArrayList<Integer> mappings = new ArrayList<>( allowedMappings.get( id ) );
				mappings.retainAll( available );
				if ( mappings.isEmpty() ) {
					LOGGER.error( "Entity {} does not have a valid mapping!", Registry.ENTITY_TYPE.getId( Registry.ENTITY_TYPE.get( id ) ) );
					continue;
				}

				int mapTo = random.nextInt( mappings.size() );
				randomizer.put( mappings.get( mapTo ), id );
				compliment.put( id, mappings.get( mapTo ) );
				LOGGER.info( "Mapping {} -> {}", Registry.ENTITY_TYPE.get( mappings.get( mapTo ) ), Registry.ENTITY_TYPE.get( id ) );
				available.remove( mappings.get( mapTo ) );
				ids.remove( ( Integer ) id );
			}
		}

		for ( int id : ids ) {
			if ( available.isEmpty() ) { //should never be empty
				LOGGER.error( "Entity {} does not have a valid mapping!", Registry.ENTITY_TYPE.getId( Registry.ENTITY_TYPE.get( id ) ) );
				break;
			}

			int mapTo = random.nextInt( available.size() );
			randomizer.put( available.get( mapTo ), id );
			compliment.put( id, available.get( mapTo ) );
			LOGGER.info( "Mapping {} -> {}", Registry.ENTITY_TYPE.get( available.get( mapTo ) ), Registry.ENTITY_TYPE.get( id ) );
			available.remove( mapTo );
		}
	}
}
