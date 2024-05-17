package com.hbm.dim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm.dim.trait.CBT_Atmosphere;
import com.hbm.dim.trait.CelestialBodyTrait;
import com.hbm.dim.trait.CBT_Atmosphere.FluidEntry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.main.MainRegistry;
import com.hbm.util.AstronomyUtil;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class CelestialBody {
	
	/**
	 * Stores planet data in a tree structure, allowing for bodies orbiting bodies
	 * Unit suffixes added when they differ from SI units, for clarity
	 */

	public String name;

	public int dimensionId = 0;

	public boolean canLand = false; // does this body have an associated dimension and a solid surface?

	public float massKg = 0;
	public float radiusKm = 0;
	public float semiMajorAxisKm = 0; // Distance to the parent body
	private int rotationalPeriod = 6 * 60 * 60; // Day length in seconds

	public float axialTilt = 0;

	public int processingLevel = 0; // What level of technology can locate this body?

	public ResourceLocation texture = null;
	public float[] color = new float[] {0.4F, 0.4F, 0.4F}; // When too small to render the texture

	public String tidallyLockedTo = null;

	public List<CelestialBody> satellites = new ArrayList<CelestialBody>(); // moon boyes
	public CelestialBody parent = null;

	private HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = new HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait>();

	public CelestialBody(String name) {
		this.name = name;
		this.texture = new ResourceLocation("hbm:textures/misc/space/" + name + ".png");

		nameToBodyMap.put(name, this);
	}

	public CelestialBody(String name, int id) {
		this(name);
		this.dimensionId = id;
		this.canLand = true;

		dimToBodyMap.put(id, this);
	}



	// Chainables for construction

	public CelestialBody withMassRadius(float kg, float km) {
		this.massKg = kg;
		this.radiusKm = km;
		return this;
	}

	public CelestialBody withSemiMajorAxis(float km) {
		this.semiMajorAxisKm = km;
		return this;
	}

	public CelestialBody withRotationalPeriod(int seconds) {
		this.rotationalPeriod = seconds;
		return this;
	}

	public CelestialBody withAxialTilt(float degrees) {
		this.axialTilt = degrees;
		return this;
	}

	public CelestialBody withProcessingLevel(int level) {
		this.processingLevel = level;
		return this;
	}

	public CelestialBody withTexture(String path) {
		this.texture = new ResourceLocation(path);
		return this;
	}

	public CelestialBody withColor(float... color) {
		this.color = color;
		return this;
	}

	public CelestialBody withTidalLockingTo(String name) {
		tidallyLockedTo = name;
		return this;
	}

	public CelestialBody withSatellites(CelestialBody... bodies) {
		Collections.addAll(satellites, bodies);
		for(CelestialBody body : bodies) {
			body.parent = this;
		}
		return this;
	}

	public CelestialBody withTraits(CelestialBodyTrait... traits) {
		for(CelestialBodyTrait trait : traits) this.traits.put(trait.getClass(), trait);
		return this;
	}

	// /Chainables



	// Terraforming - trait overrides
	// If trait overrides exist, delete existing traits from the world, and replace them with the saved ones

	public static void setTraits(World world, CelestialBodyTrait... traits) {
		CelestialBodyWorldSavedData traitsData = CelestialBodyWorldSavedData.get(world);
		
		traitsData.setTraits(traits);

		// Mark the saved data as dirty to ensure changes are saved
		traitsData.markDirty();
	}

	public static void setTraits(World world, Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
		setTraits(world, traits.values().toArray(new CelestialBodyTrait[0]));
	}

	// Gets a clone of the body traits that are SAFE for modifying
	public static HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits(World world) {
		CelestialBodyWorldSavedData traitsData = CelestialBodyWorldSavedData.get(world);
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = traitsData.getTraits();

		if(currentTraits == null) {
			currentTraits = new HashMap<>();
			CelestialBody body = CelestialBody.getBody(world);
			for(CelestialBodyTrait trait : body.traits.values()) {
				currentTraits.put(trait.getClass(), trait);
			}
		}

		return currentTraits;
	}

	public static void modifyTraits(World world, CelestialBodyTrait... traits) {
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);
		
		for(CelestialBodyTrait trait : traits) {
			currentTraits.put(trait.getClass(), trait);
		}

		// Sun traits should be set on the sun, ideally
		// Why? Otherwise we'll get a desync!
		// Permasync will need to transmit that data too
		// (Also the implementation that was here was wiping ALL planet terraforming)

		// God Damn The Sun

		setTraits(world, currentTraits);
	}

	public static void clearTraits(World world) {
		CelestialBodyWorldSavedData traitsData = CelestialBodyWorldSavedData.get(world);

		traitsData.clearTraits();
		traitsData.markDirty();
	}

	// Conversion rate from millibuckets to atmospheres
	// 1 atmosphere is 1 terabucket
	private static final double MB_PER_ATM = 1_000_000_000_000D * 1_000D;

	public static void consumeGas(World world, FluidType fluid, double amount) {
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

		CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);

		int emptyIndex = -1;
		for(int i = 0; i < atmosphere.fluids.size(); i++) {
			FluidEntry entry = atmosphere.fluids.get(i);
			if(entry.fluid == fluid) {
				entry.pressure -= amount / MB_PER_ATM;
				emptyIndex = entry.pressure <= 0 ? i : -1;
			}
		}

		if(emptyIndex > 0) {
			atmosphere.fluids.remove(emptyIndex);

			if(atmosphere.fluids.size() == 0) {
				currentTraits.remove(CBT_Atmosphere.class);
			}
		}


		setTraits(world, currentTraits);
	}

	public static void emitGas(World world, FluidType fluid, double amount) {
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> currentTraits = getTraits(world);

		CBT_Atmosphere atmosphere = (CBT_Atmosphere) currentTraits.get(CBT_Atmosphere.class);
		if(atmosphere == null) {
			atmosphere = new CBT_Atmosphere();
			currentTraits.put(CBT_Atmosphere.class, atmosphere);
		}

		boolean hasFluid = false;
		for(FluidEntry entry : atmosphere.fluids) {
			if(entry.fluid == fluid) {
				entry.pressure += amount / MB_PER_ATM;
				hasFluid = true;
			}
		}

		if(!hasFluid) {
			MainRegistry.logger.info("ADDING A FLUID!!!");
			atmosphere.fluids.add(new FluidEntry(fluid, amount / MB_PER_ATM));
		}

		setTraits(world, currentTraits);
	}

	// /Terraforming



	// Static getters
	// A lot of these are member getters but without having to check the celestial body exists
	// If it doesn't exist, return the overworld as the default, may cause issues with terraforming the overworld

	private static HashMap<Integer, CelestialBody> dimToBodyMap = new HashMap<Integer, CelestialBody>();
	private static HashMap<String, CelestialBody> nameToBodyMap = new HashMap<String, CelestialBody>();

	public static CelestialBody getBody(String name) {
		CelestialBody body = nameToBodyMap.get(name);
		return body != null ? body : dimToBodyMap.get(0);
	}

	public static CelestialBody getBody(int id) {
		CelestialBody body = dimToBodyMap.get(id);
		return body != null ? body : dimToBodyMap.get(0);
	}

	public static CelestialBody getBody(World world) {
		return getBody(world.provider.dimensionId);
	}

	public static CelestialBody getStar(World world) {
		return getBody(world).getStar();
	}

	public static CelestialBody getPlanet(World world) {
		return getBody(world).getPlanet();
	}

	public static double getRotationalPeriod(World world) {
		return getBody(world).getRotationalPeriod();
	}

	public static float getSemiMajorAxis(World world) {
		return getBody(world).semiMajorAxisKm;
	}

	public static boolean hasTrait(World world, Class<? extends CelestialBodyTrait> trait) {
		return getBody(world).hasTrait(trait);
	}
	
	public static <T extends CelestialBodyTrait> T getTrait(World world, Class<? extends T> trait) {
		return getBody(world).getTrait(trait);
	}

	// /Statics



	public String getUnlocalizedName() {
		return name;
	}

	public CelestialBody getStar() {
		CelestialBody body = this;
		while(body.parent != null)
			body = body.parent;

		return body;
	}

	public CelestialBody getPlanet() {
		if(this.parent == null) return this;
		CelestialBody body = this;
		while(body.parent.parent != null)
			body = body.parent;

		return body;
	}

	// Returns the day length in ticks, adjusted for the 20 minute minecraft day
	public double getRotationalPeriod() {
		return (double)rotationalPeriod * (AstronomyUtil.DAY_FACTOR / (double)AstronomyUtil.TIME_MULTIPLIER) * 20;
	}

	// Returns the year length in days, derived from semi-major axis
	public double getOrbitalPeriod() {
		double semiMajorAxis = semiMajorAxisKm * 1_000;
		double orbitalPeriod = 2 * Math.PI * Math.sqrt((semiMajorAxis * semiMajorAxis * semiMajorAxis) / (AstronomyUtil.GRAVITATIONAL_CONSTANT * parent.massKg));
		return orbitalPeriod / (double)AstronomyUtil.SECONDS_IN_KSP_DAY;
	}

	// Get the gravitational force at the surface, derived from mass and radius
	public float getSurfaceGravity() {
		float radius = radiusKm * 1000;
		return AstronomyUtil.GRAVITATIONAL_CONSTANT * massKg / (radius * radius);
	}

	
	public boolean hasTrait(Class<? extends CelestialBodyTrait> trait) {
		return getTraits().containsKey(trait);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends CelestialBodyTrait> T getTrait(Class<? extends T> trait) {
		return (T) getTraits().get(trait);
	}

	private HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> getTraits() {
		World world = DimensionManager.getWorld(dimensionId);
		HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits = CelestialBodyWorldSavedData.getTraits(world);

		if(traits != null)
			return traits;
			
		return this.traits;
	}

}
