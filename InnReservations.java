import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.time.temporal.ChronoUnit;

/*
Introductory JDBC examples based loosely on the BAKERY dataset from CSC 365 labs.

-- MySQL setup:
drop table if exists hp_goods, hp_customers, hp_items, hp_receipts;
create table hp_goods as select * from BAKERY.goods;
create table hp_customers as select * from BAKERY.customers;
create table hp_items as select * from BAKERY.items;
create table hp_receipts as select * from BAKERY.receipts;

grant all on amigler.hp_goods to hasty@'%';
grant all on amigler.hp_customers to hasty@'%';
grant all on amigler.hp_items to hasty@'%';
grant all on amigler.hp_receipts to hasty@'%';

-- Shell init:
export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/winter2020?autoReconnect=true\&useSSL=false
export HP_JDBC_USER=jmustang
export HP_JDBC_PW=...
*/

public class InnReservations {
	public static void main(String[] args) {
		System.out.println("Welcome to the final project!");
		String resp = "";
		Scanner scanner = new Scanner(System.in);
		System.out.println(
			"1: Rooms and Rates" + 
			"\n2: Reservations" + 
			"\n3: Reservation Change" + 
			"\n4: Reservation Cancellation" + 
			"\n5: Detailed Reservation Information" + 
			"\n6: Revenue" + 
			"\nq: quit");
		System.out.print("Please choose your service number: ");
		do {

			if (scanner.hasNext())
				resp = scanner.next();
			if (resp.equals("q")) {
				System.out.println("Bye!");
				break;
			}
			try {
				InnReservations hp = new InnReservations();
				int demoNum = Integer.parseInt(resp);    
				switch (demoNum) {
					// case 0: hp.reset(); break;
					case 1: hp.demo1(); break;
					case 2: hp.demo2(); break;
					case 3: hp.demo3(); break;
					case 4: hp.demo4(); break;
					case 5: hp.demo5(); break;
					case 6: hp.demo6(); break;
				}
				System.out.println(
					"---------------------------------------" + 
					"\nchoose from the following services: " + 
 					"\n1: Rooms and Rates" + 
					"\n2: Reservations" + 
					"\n3: Reservation Change" + 
					"\n4: Reservation Cancellation" + 
					"\n5: Detailed Reservation Information" + 
					"\n6: Revenue" + 
					"\nq: quit");
				System.out.print("\nPlease choose your service number: ");
			} catch (SQLException e) {
				System.err.println("SQLException: " + e.getMessage());
			} catch (Exception e2) {
				System.err.println("Exception: " + e2.getMessage());
			}
		} while(!resp.equals("q") && scanner.hasNext());
		scanner.close();
	}

	// FR1: Rooms and Rates.
	// done
	private void demo1() throws SQLException {
		System.out.println("\nFR1: Rooms and Rates.");		
		// Step 0: Load MySQL JDBC Driver
		// No longer required as of JDBC 2.0  / Java 6
		// try{
		// 	Class.forName("com.mysql.jdbc.Driver");
		// 	System.out.println("MySQL JDBC Driver loaded");
		// } catch (ClassNotFoundException ex) {
		// 	System.err.println("Unable to load JDBC Driver");
		// 	System.exit(-1);
		// }
		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
								System.getenv("HP_JDBC_USER"),
								System.getenv("HP_JDBC_PW"))) {
			// Step 2: Construct SQL statement
			String sql = "with \n" +
			"durations as (\n" +
			"    select Room, Checkin, Checkout, \n" +
			"    case \n" +
			"        when datediff(curdate(), checkin) > 180 and datediff(curdate(), checkout) > 0\n" +
			"            then 180-(datediff(curdate(), checkout))\n" +
			"        when datediff(curdate(), checkin) <= 180 and datediff(curdate(), checkout) < 0\n" +
			"            then datediff(curdate(), checkin)\n" +
			"        when datediff(curdate(), checkin) > 180 and datediff(curdate(), checkout) < 0\n" +
			"            then 180\n" +
			"        else\n" +
			"            datediff(checkout, checkin)\n" +
			"    end as duration\n" +
			"    from rzhang21.lab7_reservations join rzhang21.lab7_rooms\n" +
			"    on Room = RoomCode\n" +
			"    where datediff(curdate(), checkout) <= 180\n" +
			"    order by duration desc\n" +
			"), \n" +
			"popularity as (\n" +
			"    select Room, round(sum(duration)/180, 2) as popularity, sum(duration) as occupiedLength from durations\n" +
			"    group by Room\n" +
			"    order by popularity desc\n" +
			"), \n" +
			"nextAvailable as (\n" +
			"    select Room, \n" +
			"    case\n" +
			"        when datediff(curdate(), checkout) < 0\n" +
			"            then checkout\n" +
			"        else\n" +
			"            curdate()\n" +
			"    end as nextAvailable\n" +
			"    from durations\n" +
			"), \n" +
			"nextAvailableMax as (\n" +
			"    select Room, max(nextAvailable) as nextCheckin from nextAvailable group by Room\n" +
			"), \n" +
			"allDurations as (\n" +
			"    select Room, Checkout, datediff(checkout, checkin) as duration from rzhang21.lab7_rooms join rzhang21.lab7_reservations\n" +
			"    on Room = RoomCode\n" +
			"), \n" +
			"mostRecentCheckout as (\n" +
			"    select Room, max(checkout) as mostRecentCheckout from allDurations\n" +
			"    group by Room\n" +
			"), \n" +
			"mostRecentDuration as (\n" +
			"    select mostRecentCheckout.Room, duration from mostRecentCheckout join allDurations on mostRecentCheckout.room = allDurations.room\n" +
			"    where allDurations.checkout = mostRecentCheckout.mostRecentCheckout\n" +
			")\n" +
			"\n" +
			"select n.room, popularity,  nextcheckin as nextAvailableCheckIn, occupiedLength, m.duration as mostRencetCompleteDuration\n" +
			"    from nextAvailableMax as n join popularity as p on n.room = p.room\n" +
			"    join mostRecentDuration as m on m.room = n.room\n" +
			"    order by popularity desc";
			// Step 3: (omitted in this example) Start transaction
			// Step 4: Send SQL statement to DBMS
			try (Statement stmt = conn.createStatement();
				// Step 5: Receive results
				ResultSet rs = stmt.executeQuery(sql)){
				System.out.println("Room      Popularity     Next Available Check-in      Most Recent Complete Duration");
				while (rs.next()) {
					String room = rs.getString("room");
					float popularity = rs.getFloat("popularity");
					String nextAvailableCheckIn = rs.getString("nextAvailableCheckIn");
					int mostRencetCompleteDuration = rs.getInt("mostRencetCompleteDuration");
					System.out.format("%s        %.2f          %s                            %d %n", room, popularity, nextAvailableCheckIn, mostRencetCompleteDuration);
				}
			}
			// Step 6: (omitted in this example) Commit or rollback transaction
		}
		// Step 7: Close connection (handled by try-with-resources syntax)
	}

	// FR2: Reservations
	private void demo2() throws SQLException {
		System.out.println("\nFR2: Making Reservations");
		String firstName;
		String lastName;
		String roomCode;
		String bedType;
		String begin;		//begin date
		String end;			//end date
		int children; 	//number of children
		int adult;			//number of adults

		//prompt and take inputs from the user. 
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter your First Name: ");
		firstName = scanner.next();
		System.out.print("Enter your Last Name: ");
		lastName = scanner.next();	
		System.out.print("Enter your Desired Room Code(Enter 'Any' if there is no preference): ");
		roomCode = scanner.next();	
		System.out.print("Enter your Desired Bed Type(Enter 'Any' if there is no preference):  ");
		bedType = scanner.next();	
		System.out.print("Enter your Begin Date(YYYY-MM-DD): ");
		begin = scanner.next();
		System.out.print("Enter your Leaving Date(YYYY-MM-DD): ");
		end = scanner.next();	
		System.out.print("Enter Number of Children: ");
		children = scanner.nextInt();
		System.out.print("Enter Number of Adults: ");
		adult = scanner.nextInt();

		int totalPeople = children + adult;
		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
								System.getenv("HP_JDBC_USER"),
								System.getenv("HP_JDBC_PW"))) {
			int maxPeople = 0; //largest possible amount of people a room can take. 
			// set the maxPeople from the database
			try(Statement stmt = conn.createStatement()) {
				String sql = "select max(maxOcc) as mxm from rzhang21.lab7_rooms";
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next())
					maxPeople = rs.getInt("mxm");
			}
			//if user's total people > largest capacity of hotel, warn the user and exit the current serving session. 
			if (totalPeople > maxPeople) {
				System.out.println("Sorry, our largest room can only take " + maxPeople + " people");
				return;
			}

			String sql = "with \n" +
				"occTime as (\n" +
				"    select Room from rzhang21.lab7_rooms join rzhang21.lab7_reservations\n" +
				"    on room = roomCode\n" +
				"    where (? <= checkout and ? >= checkin) or (? <= checkout and ? >= checkin)\n" +
				"), \n" +
				"availableRoom as (\n" +
				"    select RoomCode, BedType from rzhang21.lab7_rooms\n" +
				"    where RoomCode not in (select * from occTime)\n" +
				"    and maxOcc >= ?\n" +
				")\n" +
				"select RoomCode from availableRoom\n";

			StringBuffer sb = new StringBuffer(sql);
			List<Object> params = new ArrayList<Object>();
			List<String> rooms = new ArrayList<>();
			params.add(java.sql.Date.valueOf(LocalDate.parse(begin)));
			params.add(java.sql.Date.valueOf(LocalDate.parse(begin)));
			params.add(java.sql.Date.valueOf(LocalDate.parse(end)));
			params.add(java.sql.Date.valueOf(LocalDate.parse(end)));
			params.add(totalPeople);
			if (!"any".equalsIgnoreCase(roomCode)) {
				sb.append(" WHERE RoomCode = ?");
				params.add(roomCode);
			}
			if (!"any".equalsIgnoreCase(bedType)) {
				sb.append(" AND BedType = ?");
				params.add(bedType);
			}
			//search for qualified rooms
			try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
				int i = 1;
				for (Object o: params) {
					pstmt.setObject(i++, o);
				}
				//execute the sql select and display rooms
				try (ResultSet rs = pstmt.executeQuery()) {
					if (!rs.next()) {
						//remove the least important factors: room preference and bed type preference. 
						params.remove(roomCode);
						params.remove(bedType);
						StringBuffer sb2 = new StringBuffer(sql);
						sb2.append("limit 5\n");
						try (PreparedStatement pstmt2 = conn.prepareStatement(sb2.toString())) {
							i = 1;
							for (Object o:params) {
								pstmt2.setObject(i++, o);
							}
							try (ResultSet rs2 = pstmt2.executeQuery()) {
								i = 1;
								System.out.println("No rooms available, now display 5 room recommendations \n if less than 5 then there is no room for the date you give");
								while (rs2.next()) {
									String code = rs2.getString("roomcode");
									rooms.add(code);
									System.out.println((i++) + ". " + code);
								}
								if (i == 1) {
									System.out.println("current algorithm cannot find nearby rooms based on your requirement, maybe change your searching requirement and try again!");
									return;
								}
							}
						}
					} else {
						//display all available rooms. 
						i = 1;
						System.out.println("available rooms: ");
						while (rs.next()) {
							String code = rs.getString("RoomCode");
							rooms.add(code);
							System.out.println((i++) + ". " + code);
						}

					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
				conn.rollback();
			}
			//prompt the user to enter a room. 
			System.out.println("Enter the room index you want to reserve (1, 2, 3, etc.)");
			int room = scanner.nextInt();
			int baseRate = 0;
			//calculate the total cost, average rate for the choice that the user has made
			String codeName = rooms.get(room - 1);
			String roomName = "";
			String bedName = "";
			String requestBaseRateSql = "select RoomCode, basePrice, RoomName, BedType from rzhang21.lab7_rooms\n" +
			"where roomcode = ?";
			try (PreparedStatement pstmt3 = conn.prepareStatement(requestBaseRateSql)) {
				pstmt3.setString(1, codeName);
				try (ResultSet rs3 = pstmt3.executeQuery()) {
					if (rs3.next()) {
						baseRate = rs3.getInt("baseprice");
						roomName = rs3.getString("roomname");
						bedName = rs3.getString("bedtype");
					}
				}
			}
			float totalCost = 0;
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-mm-dd");
			LocalDate terminal = LocalDate.parse(end);
			for (LocalDate date = LocalDate.parse(begin); date.isBefore(terminal); date = date.plusDays(1)) {
				if (isWeekend(date)) {
					totalCost += 1.1 *  baseRate ;
				} else {
					totalCost += 1 * baseRate;
				}
    	}
			//format the average cost to the second decimal
			float averageCost = totalCost / ChronoUnit.DAYS.between(LocalDate.parse(begin), LocalDate.parse(end));
			DecimalFormat df = new DecimalFormat("#.00");  
			averageCost = Float.valueOf(df.format(averageCost));	
			//print the reservation information to the user. 
			System.out.println("reservation information:");
			System.out.printf("First Name: %s\nLast Name: %s\nRoom Code: %s\nRoom Name: %s\nBed Type: %s\nBegin Date: %s\nEnd Date: %s\nAdults: %d\nChildren: %d\nTotal Cost: $%.2f\n", 
				firstName, lastName, codeName, roomName, bedName, begin, end, adult, children, totalCost);
			//ask if the user wants the room
				System.out.println("Do you want to make a reservations? (Y/N)");
			if (scanner.next().equalsIgnoreCase("Y")) {
				int max = 0;
				try (Statement stmt = conn.createStatement()) {
					try (ResultSet rs = stmt.executeQuery("select max(code) as mxm from rzhang21.lab7_reservations")) {
						if (rs.next()) {
							max = rs.getInt("mxm");
						}
					}
				}
				String insertionSql = "insert into rzhang21.lab7_reservations (Code, Room, Checkin, Checkout, Rate, LastName, Firstname, adults, kids) \n" +
				"value (?, ?, ?, ?, ?, ?, ?, ?, ?);";
				List<Object> insertList = new ArrayList<>();
				insertList.add(++max);
				insertList.add(codeName);
				insertList.add(java.sql.Date.valueOf(LocalDate.parse(begin)));
				insertList.add(java.sql.Date.valueOf(LocalDate.parse(end)));
				insertList.add(averageCost);
				insertList.add(lastName);
				insertList.add(firstName);
				insertList.add(adult);
				insertList.add(children);
				try (PreparedStatement stmt = conn.prepareStatement(insertionSql)) {
					int i =1;
					for (Object o: insertList) {
						stmt.setObject(i, o);
						i++;
					}
					stmt.executeUpdate();
					System.out.println("Reservation has made, your reservation code is " + max);
				}
			} else {
				System.out.println("Your request has been cancelled. ");
			}
		}
		// Step 7: Close connection (handled by try-with-resources syntax)
	}

	private static boolean isWeekend(LocalDate ld) {
		DayOfWeek d = ld.getDayOfWeek();
		return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
	}	

	
	// FR3: Reservation Change
	private void demo3() throws SQLException {
		System.out.println("FR3: Reservation Change");	
		Scanner scanner = new Scanner(System.in);	
		String firstName = "", lastName = "", begin = "", end = "", room = "";
		int child = 0, adult = 0;
		int reservation = 0;
		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
								System.getenv("HP_JDBC_USER"),
								System.getenv("HP_JDBC_PW"))) {
			//request reservation code and get information from the user. 
			try (Statement stmt = conn.createStatement()) {
				System.out.println("Enter your reservation code: ");
				reservation = scanner.nextInt();
				String sql = "select * from rzhang21.lab7_reservations where code = " + reservation;
				try (ResultSet rs = stmt.executeQuery(sql)) {
					if (rs.next()) {
						room = rs.getString("Room");
						firstName = rs.getString("firstname");
						lastName = rs.getString("lastname");
						begin = rs.getString("CheckIn");
						end = rs.getString("checkout");
						child = rs.getInt("kids");
						adult = rs.getInt("adults");
					}
				}
			}
			System.out.println("Enter your Information, enter NC if no change");
			//prompt the user to enter the data they want to give
			System.out.println("Enter First Name: ");
			String rspfirstName = scanner.next();
			System.out.println("Enter Last Name: ");
			String rsplastName = scanner.next();
			System.out.println("Enter Begin Date: ");
			String rspbegin = scanner.next();
			System.out.println("Enter End Date: ");
			String rspend = scanner.next();
			System.out.println("Enter Children Number: ");
			String rspchild = scanner.next();
			System.out.println("Enter Adult Number: ");
			String rspadult = scanner.next();
			firstName = rspfirstName.equalsIgnoreCase("NC") ? firstName : rspfirstName;
			lastName = rsplastName.equalsIgnoreCase("NC") ? lastName : rsplastName;
			begin = rspbegin.equalsIgnoreCase("NC") ? begin : rspbegin;
			end = rspend.equalsIgnoreCase("NC") ? end : rspend;
			child = rspchild.equalsIgnoreCase("NC") ? child : Integer.parseInt(rspchild);
			adult = rspadult.equalsIgnoreCase("NC") ? adult : Integer.parseInt(rspadult);
			//see if there is any confliction for the new reservation
			//check date and people number. 
			String sql = "select (? >= checkin and ? <= checkout) or (? >= checkin and ? <= checkout) or (? > maxOcc) as conflict\n" +
				"    from rzhang21.lab7_reservations  join rzhang21.lab7_rooms \n" +
				"    on Room = RoomCode\n" +
				"    where Room = (select room from rzhang21.lab7_reservations where code =" + reservation + ") and code != " + reservation + ";";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				List<Object> params = new ArrayList<>();
				int i = 1;
				params.add(java.sql.Date.valueOf(LocalDate.parse(begin)));
				params.add(java.sql.Date.valueOf(LocalDate.parse(begin)));
				params.add(java.sql.Date.valueOf(LocalDate.parse(end)));
				params.add(java.sql.Date.valueOf(LocalDate.parse(end)));
				params.add(child + adult);
				for (Object o : params) {
					pstmt.setObject(i, o);
					i++;
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					boolean conflict = false;
					while (rs.next()) {
						conflict = conflict || rs.getBoolean("conflict");
					}
					if (conflict) {
						System.out.println("your request is conflicting with other scedules, please consider change");
						return;
					}
				}
			}
			//the program wuold have returned if there is a conflict. 
			//now update the request from the user
			String updateSql = "UPDATE rzhang21.lab7_reservations\n" +
				"SET firstname = ?, lastname = ?, checkin = ?, checkout = ?, kids = ?, adults = ?\n" +
				"WHERE code = "  + reservation;
			try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
				pstmt.setString(1, firstName);
				pstmt.setString(2, lastName);
				pstmt.setObject(3, java.sql.Date.valueOf(LocalDate.parse(begin)));
				pstmt.setObject(4, java.sql.Date.valueOf(LocalDate.parse(end)));
				pstmt.setInt(5, child);
				pstmt.setInt(6, adult);
				try {
					pstmt.executeUpdate();
					System.out.println("Update succeed!");
				} catch (SQLException e) {
					System.out.println("Update error occured.");
				}
			}
		}
	}

	// Reservation Cancellation  
	private void demo4() throws SQLException {
		System.out.println("Reservation Cancellation\r\n");
			
		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
								System.getenv("HP_JDBC_USER"),
								System.getenv("HP_JDBC_PW"))) {
			// Step 2: Construct SQL statement
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter your reservation code: ");
			int reservationCode = scanner.nextInt();
			String sql = "delete from rzhang21.lab7_reservations where code = " + reservationCode + ";";
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate(sql);
				System.out.println("The reservation has been cancelled!");
			}
		}
		// Step 7: Close connection (handled implcitly by try-with-resources syntax)
	}

	// Demo5 - Construct a query using PreparedStatement
	private void demo5() throws SQLException {
		System.out.println("FR5: Detailed Reservation Information");
		System.out.println("Enter 'N' if not trying to fill anything");
		Scanner scanner = new Scanner(System.in);	
		String firstName = "", lastName = "", begin = "", end = "", room = "";
		int reservation = 0;
		System.out.println("Enter First Name: ");
		String rspfirstName = scanner.next();
		System.out.println("Enter Last Name: ");
		String rsplastName = scanner.next();
		System.out.println("Enter Begin Date: ");
		String rspbegin = scanner.next();
		System.out.println("Enter End Date: ");
		String rspend = scanner.next();
		System.out.println("Enter Room Code: ");
		String rspRoom = scanner.next();
		System.out.println("Enter Reservation Code: ");
		String rspCode = scanner.next();
		firstName = rspfirstName.equalsIgnoreCase("N") ? firstName : rspfirstName;
		lastName = rsplastName.equalsIgnoreCase("N") ? lastName : rsplastName;
		begin = rspbegin.equalsIgnoreCase("N") ? begin : rspbegin;
		end = rspend.equalsIgnoreCase("N") ? end : rspend;
		reservation = rspCode.equalsIgnoreCase("N") ? reservation : Integer.parseInt(rspCode);
		room = rspRoom.equalsIgnoreCase("N") ? room : rspRoom;
		// Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
							System.getenv("HP_JDBC_USER"),
							System.getenv("HP_JDBC_PW"))) {
			List<Object> lst = new ArrayList<>();
			StringBuffer sqlSearch = new StringBuffer("select *\n" +
				"    from rzhang21.lab7_reservations" +
				"    where firstname like ? and lastname like ? ");
			lst.add(firstName + "%");
			lst.add(lastName + "%");
			if (!begin.equals("")) {
				sqlSearch.append("and checkin <= ?");
				lst.add(java.sql.Date.valueOf(LocalDate.parse(begin)));
			}
			if (!end.equals("")) {
				sqlSearch.append("and checkout >= ?");
				lst.add(java.sql.Date.valueOf(LocalDate.parse(end)));
			}
			if (reservation != 0 ) {
				sqlSearch.append("and code like  ?");
				lst.add(reservation);
			}
			try (PreparedStatement pstmt = conn.prepareStatement(sqlSearch.toString())) {
				int i = 1;
				for (Object o: lst) {
					pstmt.setObject(i++, o);
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					System.out.println("CODE    ROOM     CHECKIN          CHECKOUT         RATE           LASTNAME          FIRSTNAME       ADULTS           KIDS");
					while (rs.next()) {
						System.out.printf("%d %5s %15s %16s %12.2f %16s %18s %12d %14d\n", 
							rs.getInt("Code"), rs.getString("room"), rs.getString("checkin"), rs.getString("checkout"), rs.getFloat("Rate"), 
							rs.getString("lastname"), rs.getString("firstname"), rs.getInt("adults"), rs.getInt("kids"));
					}
				}
			}
		}
	}

	private static List<LocalDate> getMonthList() {
		int year = 2022;
		LocalDate Jan = LocalDate.of(year,1, 1);
		LocalDate Feb = LocalDate.of(year,2, 1);
		LocalDate Mar = LocalDate.of(year,3, 1);
		LocalDate Apr = LocalDate.of(year,4, 1);
		LocalDate May = LocalDate.of(year,5, 1);
		LocalDate Jun = LocalDate.of(year,6, 1);
		LocalDate Jul = LocalDate.of(year,7, 1);
		LocalDate Aug = LocalDate.of(year,8, 1);
		LocalDate Sep = LocalDate.of(year,9, 1);
		LocalDate Oct = LocalDate.of(year,10, 1);
		LocalDate Nov = LocalDate.of(year,11, 1);
		LocalDate Dec = LocalDate.of(year,12, 1);
		LocalDate Jan2 = LocalDate.of(year + 1,1, 1);
		List<LocalDate> months = new ArrayList<>();
		months.add(Jan);
		months.add(Feb);
		months.add(Mar);
		months.add(Apr);
		months.add(May);
		months.add(Jun);
		months.add(Jul);
		months.add(Aug);
		months.add(Sep);
		months.add(Oct);
		months.add(Nov);
		months.add(Dec);
		months.add(Jan2);
		return months;
	}

	//FR6: Revenue
	private void demo6() throws SQLException {
		System.out.println("FR6: Revenue");
		List<LocalDate> months = getMonthList();
		Map<String, float[]> revenue = new HashMap<>();
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),System.getenv("HP_JDBC_USER"),System.getenv("HP_JDBC_PW"))) {
			try (Statement stmt = conn.createStatement()) {
				try (ResultSet rs = stmt.executeQuery("select distinct room from rzhang21.lab7_reservations")) {
					while (rs.next()) {
						float[] floats = new float[13];
						for (int i =0; i< 13; i++) {
							floats[i] = 0;
						}
 						revenue.put(rs.getString("room"), new float[13]);
					}
				}
			}	
			String sql = "select Room, checkin, checkout, rate from rzhang21.lab7_reservations\n" +
			"where checkout >= '2022-01-01'";
			try (Statement stmt = conn.createStatement()) {
				try (ResultSet rs = stmt.executeQuery(sql)) {
					while (rs.next()) {
						float[] revens = revenue.get(rs.getString("room"));
						float rate = rs.getFloat("rate");
						LocalDate begin = LocalDate.parse(rs.getString("checkin"));
						LocalDate end = LocalDate.parse(rs.getString("checkout"));
						for (int i = 0; i < 12; i++) {
							float rev = 0;
							if (begin.compareTo(months.get(i)) == 1 && end.compareTo(months.get(i+1)) == -1) {
								rev = rate * ChronoUnit.DAYS.between(begin, end);
							} else if (begin.compareTo(months.get(i)) == -1 && end.compareTo(months.get(i)) == 1 && end.compareTo(months.get(i+1)) == -1) {
								rev = rate * ChronoUnit.DAYS.between(months.get(i), end);
							} else if (begin.compareTo(months.get(i)) == 1 && end.compareTo(months.get(i+1)) == 1 && begin.compareTo(months.get(i+1)) == -1) {
								rev = rate * ChronoUnit.DAYS.between(begin, months.get(i+1));
							} else if (begin.compareTo(months.get(i)) == -1 && end.compareTo(months.get(i+1)) == 1) {
								rev = rate * ChronoUnit.DAYS.between(months.get(i), months.get(i+1));
							}
							// System.out.println(rs.getString("room") + "    month: " + i + "  revenue " + rev + "   rate: " + rate);
							revens[i] += rev;
							revens[12] += rev;
						}
					}
				}
			}
			System.out.printf("%s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s%10s\n", "Room", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Total");
			for (String key : revenue.keySet()) {
				float[] revens = revenue.get(key);
				System.out.printf("%s%10.2f%10.2f%10.2f%10.2f%10.2f%10.2f%10.2f%10.2f%10.2f%10.2f%10.2f%10.2f%10.2f\n", 
					key, revens[0], revens[1], revens[2], revens[3], 
					revens[4], revens[5], revens[6], revens[7], 
					revens[8], revens[9], revens[10], revens[11], revens[12]);
			}
		}
	}
}
