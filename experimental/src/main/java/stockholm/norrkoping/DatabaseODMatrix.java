package stockholm.norrkoping;
/**
 * @author Rasmus Ringdahl @ Link�ping University (rasmus.ringdahl@liu.se)
 */
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

public class DatabaseODMatrix implements ODMatrix 
{

	// Database connection variables.
	private String username;
	private String password;
	private String host;
	private int port;
	
	// Output variables.
	HashSet<String> zoneIds;
	int numberOfTimeBins = 24; // TODO: Load from the database?
	SimpleFeatureCollection odMatrix;
	
	/**
	 * This is the constructor for the DatabaseODMatrix class.
	 * 
	 * @param username - Username in the database.
	 * @param password - Password to the database.
	 * @param host	   - Host to the database.
	 * @param port     - Port to the database.
	 */
	public DatabaseODMatrix(String username, String password, String host, int port)
	{
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		
	}
	
	/**
	 * This method returns the number of timebins.
	 * @return Index of the highest time bin.
	 */
	public int getNumberOfTimeBins() 
	{
		return numberOfTimeBins;
	}

	/**
	 * This method returns the OD zone ids.
	 * The zones are cached.
	 * 
	 * @return Collection of Strings with the zoneIds.
	 */
	public Collection<String> getAllZoneIds() 
	{
		// Checking if the zones has been loaded.
		if(zoneIds == null)
		{
			// Creating a HashSet.
			zoneIds = new HashSet<String>();
			
			// Setting parameters for the database connection.
			Map<String,Object> params = new HashMap<String,Object>();
		    params.put( "dbtype", "postgis");
		    params.put( "user", username);
		    params.put( "passwd", password);
		    params.put( "host", host);
		    params.put( "port", port);
		    params.put( "database", "mode");
		    params.put( "schema", "norrkoping");
		    params.put( "Expose primary keys", "true");
	
		    try
		    {
		    	// Creates a data source to the database.
			    DataStore dataStore = DataStoreFinder.getDataStore(params);
			    
			    // Creating data filter.
			    FilterFactory2 factory = new FilterFactoryImpl();
			    ArrayList<Filter> filters = new ArrayList<Filter>();
			    Query query = new Query("zone", factory.and(filters));
			    
			    query.setSortBy(new SortBy[] {factory.sort("zone", SortOrder.ASCENDING)});
			    
			    // Getting a feature source to the zone table.
				SimpleFeatureSource source = dataStore.getFeatureSource("zone");
			    
				// Extracting the features into a collection.
				SimpleFeatureCollection collection = source.getFeatures(query);
				
				// Loops through the collection and caches the zone ids.
				SimpleFeatureIterator it = collection.features();
				try
				{
					while(it.hasNext())
					{
						SimpleFeature feature = it.next();
						zoneIds.add(feature.getAttribute("id").toString());
					}
				}
				
				// Releasing the database connection.
				finally
				{
					it.close();
					
					if(source.getDataStore() != null)
					{
						source.getDataStore().dispose();
					}
				}
				
		    }
		    catch (IOException e)
		    {
		    	throw new RuntimeException(e);
		    }
		}
		
		return zoneIds;
	}

	/**
	 * This method extracts the demand between an origin and destination for a specific hour (time bin).
	 * 
	 * @return demand.
	 */
	public double getDemandPerHour(String originZoneId, String destinationZoneId, int timeBin) 
	{
		double demand = 0;
		
		try
		{
			// Checking if the OD matrix has been loaded.
			if(odMatrix == null)
			{
				// Setting parameters for the database connection.
				Map<String,Object> params = new HashMap<String,Object>();
			    params.put( "dbtype", "postgis");
			    params.put( "user", username);
			    params.put( "passwd", password);
			    params.put( "host", host);
			    params.put( "port", port);
			    params.put( "database", "mode");
			    params.put( "schema", "norrkoping");
			    params.put( "Expose primary keys", "true");
		
			    // Creates a datasource to the database.
			    DataStore dataStore = DataStoreFinder.getDataStore(params);
			    
			    // Creating data filter.
			    FilterFactory2 factory = new FilterFactoryImpl();
			    ArrayList<Filter> filters = new ArrayList<Filter>();
			    filters.add(CQL.toFilter("matrix = 'stop'"));
			    filters.add(CQL.toFilter("dow = 2"));
			    Query query = new Query("od", factory.and(filters));
			    
			    query.setSortBy(new SortBy[] {factory.sort("origin", SortOrder.ASCENDING),
			    							  factory.sort("destination", SortOrder.ASCENDING),
			    							  factory.sort("hour", SortOrder.ASCENDING)});
			    
			    // Getting a feature source to the OD table
			    SimpleFeatureSource source = dataStore.getFeatureSource("od");
			    
			    // Extracting the features into a collection.
				odMatrix = source.getFeatures(query);
			}
			

			// Creating data filter with the requested OD and time bin..
			FilterFactory2 factory = new FilterFactoryImpl();
			ArrayList<Filter> filters = new ArrayList<Filter>();
		    filters.add(CQL.toFilter(String.format("origin = %s",originZoneId)));
		    filters.add(CQL.toFilter(String.format("destination = %s",destinationZoneId)));
		    filters.add(CQL.toFilter(String.format("hour = %d",timeBin)));
		    
			SimpleFeatureCollection filteredOD = odMatrix.subCollection(factory.and(filters));
			
			// Extracts the flow from the first feature (assuming 1 row).
			SimpleFeatureIterator it = filteredOD.features();
			try
			{
				if(it.hasNext())
				{
					SimpleFeature feature = it.next();
					demand = (Float) feature.getAttribute("flow");
				}
			}
			finally
			{
				it.close();
			}
		}		
		catch (IOException e)
	    {
			throw new RuntimeException(e);
	    } 
		catch (CQLException e) 
		{
			throw new RuntimeException(e);
		}
		
		return demand;
	}

	public static void main(String[] args) 
	{
		// Creating a DatabaseODMatrix object with credentials.  
		DatabaseODMatrix od = new DatabaseODMatrix(args[0], args[1], "localhost", 5432); // 5455);
		
		
		LocalTime tick = LocalTime.now();
		
		// Getting all zone ids.
		Collection<String> allZones = od.getAllZoneIds();
		
		LocalTime tock = LocalTime.now();
		System.out.println("Loading zones (first time) took " + java.time.temporal.ChronoUnit.MILLIS.between(tick, tock) + " ms");
		System.out.println("Total number of zones: " + allZones.size() + "\n");
		
		tick = LocalTime.now();
		
		// Getting all zone ids.
		allZones = od.getAllZoneIds();
		
		tock = LocalTime.now();
		System.out.println("Loading zones (second time) took " + java.time.temporal.ChronoUnit.MILLIS.between(tick, tock) + " ms");
		System.out.println("Total number of zones: " + allZones.size() + "\n");

		tick = LocalTime.now();
		
		// Getting demand.
		double demand = od.getDemandPerHour("1", "2", 0);
		
		
		tock = LocalTime.now();
		System.out.println("Loading demand (first time) took " + java.time.temporal.ChronoUnit.MILLIS.between(tick, tock) + " ms");
		System.out.println(String.format("Demand between 1 and 2 at time bin 0 is %.2f.\n", demand));
		tick = LocalTime.now();
		
		// Getting demand, connective queries is faster than the first one.
		demand = od.getDemandPerHour("1", "2", 8);
		
		tock = LocalTime.now();
		System.out.println("Loading demand (second time) took " + java.time.temporal.ChronoUnit.MILLIS.between(tick, tock) + " ms");
		System.out.println(String.format("Demand between 1 and 2 at time bin 8 is %.2f.\n", demand));
	}
}