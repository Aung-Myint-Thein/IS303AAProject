package test;
import java.sql.*;

// DbBean is the Data Access Object bean which connects to the MySQL database & 
// executes SQL statements. Methods here are used by the Servlets and JSP pages
// in the whole project.

public class DbBean {

    double TOTAL_CASH_IN_WORLD = 50000; // reflects the total amount of cash in the bank and payment gateway

    // change the dbURL if necessary.
    String dbURL = "jdbc:mysql://localhost/bank";
    String dbDriver = "com.mysql.jdbc.Driver";
    private Connection dbCon;
    

    // constructor
    public DbBean() {
      // do nothing
    }

    // connects to the database using root. change your database id/password here if necessary
    public boolean connect() throws ClassNotFoundException, SQLException {
        Class.forName(dbDriver);

        // login credentials to your MySQL server
        dbCon = DriverManager.getConnection(dbURL, "root", "password"); 
        return true;
    }

    // closes the DB connection
    public void close() throws SQLException {
        dbCon.close();
    }


    // insert a row into the Payment table
    // (to be called when a successful payment is made to the payment gateway
    public void recordPayment (String userId, double amt, String refId) throws SQLException{
        // perform insert
        Statement s = dbCon.createStatement();
        s.executeUpdate("INSERT INTO payments (payeeId, payment, refid) VALUES ('" + userId + "','" + amt + "','"+ refId + "')");
        return;
    }

    // get total payment made so far recorded in table payments
    public double getTotalPaymentMade () throws SQLException{
        Statement s = dbCon.createStatement();
        ResultSet r = s.executeQuery("SELECT SUM(payment) FROM payments");

        if (r == null) {
            return -1;
        }

        double balance = 0;
        while (r.next()) {
            balance = r.getDouble(1);
        }
        return balance;
    }

    // get the sum of the balance of all user accounts recorded in table accounts
    public double getTotalBalance () throws SQLException{
        Statement s = dbCon.createStatement();
        ResultSet r = s.executeQuery("SELECT SUM(balance) FROM accounts");

        if (r == null) {
            return -1;
        }

        double balance = 0;
        while (r.next()) {
            balance = r.getDouble(1);
        }
        return balance;
    }

    // checks if there are user accounts with a balance below zero (should not happen)
    // returns the no. of users with accounts below 0
    public int getNoOfAccountsWithNegativeBalance() throws SQLException {
        Statement s = dbCon.createStatement();
        ResultSet r = s.executeQuery("select count(*) from accounts where balance<0");

        int count = 0;
        while (r.next()) {
            count = r.getInt(1);
        }
        return count;
    }

    // debit an account
    public boolean debit(String userId, double amt) throws SQLException{

        // check if user ID is valie
        if (!idExists(userId)){
            return false;
        }

        // check if there is enough balance
        double balance = getBalance(userId);
        if (balance<amt){
            return false;
        }
        double newBalance = balance-amt;

        // perform debit
        Statement s = dbCon.createStatement();
        s.executeUpdate("UPDATE accounts SET balance=" + newBalance + " WHERE id='" + userId + "'");
        return true;
    }

    // credit an account
    public boolean credit(String userId, double amt) throws SQLException{

        // check if user ID is valie
        if (!idExists(userId)){
            return false;
        }

        double newBalance = getBalance(userId) + amt;

        // perform credit
        Statement s = dbCon.createStatement();
        s.executeUpdate("UPDATE accounts SET balance=" + newBalance + " WHERE id='" + userId + "'");
        return true;
    }

    // checks if a particular user ID exists in the accounts table.
    // returns true if it does. false otherwise
    private boolean idExists(String id) throws SQLException {
        Statement s = dbCon.createStatement();
        ResultSet r = s.executeQuery("select count(*) from accounts where id='" + id + "'");

        int count = 0;
        while (r.next()) {
            count = r.getInt(1);
        }
        return (count>0);
    }

    // returns the balance of a particular user ID in the accounts table
    // returns -1 if the user ID does not exist.
    public double getBalance(String id) throws SQLException {
        Statement s = dbCon.createStatement();
        ResultSet r = s.executeQuery("select balance from accounts where id='" + id + "'");

        if (r == null) {
            return -1;
        }

        double balance = 0;
        while (r.next()) {
            balance = r.getDouble(1);
        }
        return balance;
    }

    // perform a funds transfer
    // returns true if the transfer is successful
    public boolean transferFunds(String idFrom, String idTo, double amt) throws SQLException {
        if (amt <= 0) {
            return false;
        }

        boolean valid = idExists(idFrom);
        if (!valid) {
            return false;
        }
        valid = idExists(idTo);
        if (!valid) {
            return false;
        }

        double balanceFrom = getBalance(idFrom);
        double newBalanceFrom = balanceFrom - amt;
        if (newBalanceFrom < 0) {
            return false;
        }

        double balanceTo = getBalance(idTo);
        double newBalanceTo = balanceTo + amt;

        // perform transfer
        Statement s = dbCon.createStatement();
        try {
            s.execute("set autocommit=0");
            s.execute("START TRANSACTION");
            // debit
            s.executeUpdate("UPDATE accounts SET balance=" + newBalanceFrom + " WHERE id='" + idFrom + "'");
            // credit
            s.executeUpdate("UPDATE accounts SET balance=" + newBalanceTo + " WHERE id='" + idTo + "'");

            s.execute("commit");
        } catch (SQLException e) {
            s.execute("rollback");
        }
        return true;
    }

    // performs a login
    // returns -1 for failure, 0 for authenticated administrator, 1 for authenticated user
    public int login(String id, String password) throws SQLException {
        Statement s = dbCon.createStatement();

        // is this an administrator?
        ResultSet r = s.executeQuery("select count(*) from accounts where type='admin' and id='" + id + "' and password='" + password + "'");

        while (r.next()) {
            if(r.getInt(1)==0){
              // is this a user?
              r = s.executeQuery("select count(*) from accounts where type='user' and id='" + id + "' and password='" + password + "'");
              while (r.next()){
                  if (r.getInt(1)==0){
                      return -1; // no such ID
                  }
              }
              return 1; // this is a user
            }
        }
        return 0; // this is an administrator
    }

    // used to execute a generic select SQL statement
    public ResultSet execSQL(String sql) throws SQLException {
        Statement s = dbCon.createStatement();
        ResultSet r = s.executeQuery(sql);
        return (r == null) ? null : r;
    }

    // used to execute an update SQL statement.
    public int updateSQL(String sql) throws SQLException {
        Statement s = dbCon.createStatement();
        int r = s.executeUpdate(sql);
        return (r == 0) ? 0 : r;
    }
}
