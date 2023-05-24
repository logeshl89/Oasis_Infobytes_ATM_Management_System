/*
ATM
 */

package org.example;

import java.sql.*;
import java.util.Scanner;

public class ATMSystem {
//DataBase Connections
    private static final String DB_URL = "jdbc:mysql://localhost/atm_database";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "root";


    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement preparedStatement;
    private static ResultSet resultSet;
    private static Scanner scanner;

    public static void main(String[] args) {
        try {
            connectToDatabase();
            scanner = new Scanner(System.in);

            System.out.println("Welcome to the ATM System!");
            System.out.print("Enter your user ID: ");
            int userId = scanner.nextInt();

            System.out.print("Enter your user PIN: ");
            int userPin = scanner.nextInt();

            if (validateUser(userId, userPin)) {
                System.out.println("User validated successfully!");
                showMenu(userId);
            } else {
                System.out.println("Invalid user ID or PIN. Exiting...");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDatabaseResources();
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    private static void connectToDatabase() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        statement = connection.createStatement();
    }

    private static boolean validateUser(int userId, int userPin) throws SQLException {
        preparedStatement = connection.prepareStatement("SELECT * FROM users WHERE user_id = ?");
        preparedStatement.setInt(1, userId);
        resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            int storedPin = resultSet.getInt("user_pin");
            return (storedPin == userPin);
        }

        return false;
    }

    private static void showMenu(int userId) throws SQLException {
        boolean quit = false;

        while (!quit) {
            System.out.println("\nATM System Menu");
            System.out.println("1. Transaction History");
            System.out.println("2. Withdraw");
            System.out.println("3. Deposit");
            System.out.println("4. Transfer");
            System.out.println("5. Quit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1 -> showTransactionHistory(userId);
                case 2 -> performWithdrawal(userId);
                case 3 -> performDeposit(userId);
                case 4 -> performTransfer(userId);
                case 5 -> quit = true;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void showTransactionHistory(int userId) throws SQLException {
        preparedStatement = connection.prepareStatement("SELECT * FROM transactions WHERE user_id = ?");
        preparedStatement.setInt(1, userId);
        resultSet = preparedStatement.executeQuery();

        System.out.println("\nTransaction History");
        System.out.println("-------------------");
        System.out.println("ID\tType\tAmount\tDate");
        System.out.println("-------------------");

        while (resultSet.next()) {
            int transactionId = resultSet.getInt("transaction_id");
            String transactionType = resultSet.getString("transaction_type");
            double amount = resultSet.getDouble("amount");
            String transactionDate = resultSet.getString("transaction_date");

            System.out.println(transactionId + "\t" + transactionType + "\t" + amount + "\t" + transactionDate);
        }
    }

    private static void performWithdrawal(int userId) throws SQLException {
        System.out.print("Enter the amount to withdraw: ");
        double amount = scanner.nextDouble();

        double currentBalance = getUserBalance(userId);

        if (amount > currentBalance) {
            System.out.println("Insufficient balance. Withdrawal canceled.");
            return;
        }

        double updatedBalance = currentBalance - amount;
        updateBalance(userId, updatedBalance);
        insertTransaction(userId, "WITHDRAW", amount);
        System.out.println("Withdrawal successful. New balance: " + updatedBalance);
    }

    private static void performDeposit(int userId) throws SQLException {
        System.out.print("Enter the amount to deposit: ");
        double amount = scanner.nextDouble();

        double currentBalance = getUserBalance(userId);
        double updatedBalance = currentBalance + amount;
        updateBalance(userId, updatedBalance);
        insertTransaction(userId, "DEPOSIT", amount);
        System.out.println("Deposit successful. New balance: " + updatedBalance);
    }

    private static void performTransfer(int userId) throws SQLException {
        System.out.print("Enter the recipient's user ID: ");
        int recipientId = scanner.nextInt();

        if (!checkUserIdExists(recipientId)) {
            System.out.println("Recipient user ID not found.");
            return;
        }

        System.out.print("Enter the amount to transfer: ");
        double amount = scanner.nextDouble();

        double currentBalance = getUserBalance(userId);

        if (amount > currentBalance) {
            System.out.println("Insufficient balance. Transfer canceled.");
            return;
        }

        double updatedBalance = currentBalance - amount;
        updateBalance(userId, updatedBalance);
        insertTransaction(userId, "TRANSFER", amount);
        insertTransaction(recipientId, "TRANSFER", amount);
        System.out.println("Transfer successful. New balance: " + updatedBalance);
    }

    private static double getUserBalance(int userId) throws SQLException {
        preparedStatement = connection.prepareStatement("SELECT balance FROM users WHERE user_id = ?");
        preparedStatement.setInt(1, userId);
        resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getDouble("balance");
        }

        return 0.0;
    }

    private static void updateBalance(int userId, double newBalance) throws SQLException {
        preparedStatement = connection.prepareStatement("UPDATE users SET balance = ? WHERE user_id = ?");
        preparedStatement.setDouble(1, newBalance);
        preparedStatement.setInt(2, userId);
        preparedStatement.executeUpdate();
    }

    private static void insertTransaction(int userId, String transactionType, double amount) throws SQLException {
        preparedStatement = connection.prepareStatement("INSERT INTO transactions (user_id, transaction_type, amount) VALUES (?, ?, ?)");
        preparedStatement.setInt(1, userId);
        preparedStatement.setString(2, transactionType);
        preparedStatement.setDouble(3, amount);
        preparedStatement.executeUpdate();
    }

    private static boolean checkUserIdExists(int userId) throws SQLException {
        preparedStatement = connection.prepareStatement("SELECT * FROM users WHERE user_id = ?");
        preparedStatement.setInt(1, userId);
        resultSet = preparedStatement.executeQuery();

        return resultSet.next();
    }

    private static void closeDatabaseResources() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
