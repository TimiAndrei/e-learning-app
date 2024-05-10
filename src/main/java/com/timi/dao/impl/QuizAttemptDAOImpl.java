package com.timi.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import com.timi.dao.DatabaseConnection;
import com.timi.dao.QuizAttemptDAO;
import com.timi.exception.DAOException;
import com.timi.model.Question;
import com.timi.model.QuizAttempt;

public class QuizAttemptDAOImpl implements QuizAttemptDAO {
    
    private DatabaseConnection dbConnection;

    public QuizAttemptDAOImpl() {
        dbConnection = DatabaseConnection.getInstance();
    }

    @Override
    public void addQuizAttempt(QuizAttempt quizAttempt) throws DAOException {
        Connection connection = dbConnection.getConnection();

        try {
            PreparedStatement ps = null;
            if (quizAttempt.getAttemptId() == 0) {
                ps = connection.prepareStatement("INSERT INTO QuizAttempts (userId, quizId, timestamp, score, durationAttempted) VALUES (?, ?, ?, ?, ?)");
            } else {
                ps = connection.prepareStatement("INSERT INTO QuizAttempts (attemptId, userId, quizId, timestamp, score, durationAttempted) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, quizAttempt.getAttemptId());
            }

            int cnt = (quizAttempt.getAttemptId() != 0) ? 1 : 0;
            ps.setInt(1 + cnt, quizAttempt.getUserId());
            ps.setInt(2 + cnt, quizAttempt.getQuizId());
            ps.setTimestamp(3 + cnt, Timestamp.valueOf(quizAttempt.getTimestamp()));
            ps.setInt(4 + cnt, quizAttempt.getScore());
            ps.setFloat(5 + cnt, quizAttempt.getDurationAttempted());
            ps.executeUpdate();
            ps.close();
            if (quizAttempt.getQuestionsAttempted() != null) {
                String insertQuizAttemptQuestionsQuery = "INSERT INTO QuizAttemptQuestions (attemptId, questionId, selectedOptionIndex) VALUES (?, ?, ?)";
                PreparedStatement psQuizAttemptQuestions = connection.prepareStatement(insertQuizAttemptQuestionsQuery);
                for (Question question : quizAttempt.getQuestionsAttempted()) {
                    psQuizAttemptQuestions.setInt(1, quizAttempt.getAttemptId());
                    psQuizAttemptQuestions.setInt(2, question.getQuestionId());
                    psQuizAttemptQuestions.setInt(3, question.getSelectedOptionIndex());
                    psQuizAttemptQuestions.executeUpdate();
                }
                psQuizAttemptQuestions.close();
            }
        } catch (SQLException e) {
            throw new DAOException("Error adding quiz attempt", e);
        }
    }

    @Override
    public QuizAttempt getQuizAttemptById(int attemptId) throws DAOException {
        Connection connection = dbConnection.getConnection();
        QuizAttempt quizAttempt = null;

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM QuizAttempts WHERE attemptId = ?");
            ps.setInt(1, attemptId);
            var rs = ps.executeQuery();
            if (rs.next()) {
                quizAttempt = new QuizAttempt();
                quizAttempt.setAttemptId(rs.getInt("attemptId"));
                quizAttempt.setUserId(rs.getInt("userId"));
                quizAttempt.setQuizId(rs.getInt("quizId"));
                quizAttempt.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                quizAttempt.setScore(rs.getInt("score"));
                quizAttempt.setDurationAttempted(rs.getFloat("durationAttempted"));
            }
            rs.close();
            ps.close();
            connection.close();
        } catch (SQLException e) {
            throw new DAOException("Error getting quiz attempt by ID", e);
        }

        return quizAttempt;
    }

    @Override
    public List<QuizAttempt> getAllQuizAttempts() throws DAOException {
        Connection connection = dbConnection.getConnection();
        List<QuizAttempt> quizAttempts = new ArrayList<>();

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM QuizAttempts");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                QuizAttempt quizAttempt = new QuizAttempt();
                quizAttempt.setAttemptId(rs.getInt("attemptId"));
                quizAttempt.setUserId(rs.getInt("userId"));
                quizAttempt.setQuizId(rs.getInt("quizId"));
                quizAttempt.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                quizAttempt.setScore(rs.getInt("score"));
                quizAttempt.setDurationAttempted(rs.getFloat("durationAttempted"));
                quizAttempts.add(quizAttempt);
            }
            rs.close();
            ps.close();
            connection.close();
        } catch (SQLException e) {
            throw new DAOException("Error getting all quiz attempts", e);
        }

        return quizAttempts;
    }

    @Override
    public List<QuizAttempt> getQuizAttemptsByUserId(int userId) throws DAOException {
        Connection connection = dbConnection.getConnection();
        List<QuizAttempt> quizAttempts = new ArrayList<>();

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM QuizAttempts WHERE userId = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                QuizAttempt quizAttempt = new QuizAttempt();
                quizAttempt.setAttemptId(rs.getInt("attemptId"));
                quizAttempt.setUserId(rs.getInt("userId"));
                quizAttempt.setQuizId(rs.getInt("quizId"));
                quizAttempt.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                quizAttempt.setScore(rs.getInt("score"));
                quizAttempt.setDurationAttempted(rs.getFloat("durationAttempted"));
                quizAttempts.add(quizAttempt);
            }
            rs.close();
            ps.close();
            connection.close();
        } catch (SQLException e) {
            throw new DAOException("Error getting quiz attempts by user ID", e);
        }

        return quizAttempts;
    }

    @Override
    public List<QuizAttempt> getQuizAttemptsByQuizId(int quizId) throws DAOException {
        Connection connection = dbConnection.getConnection();
        List<QuizAttempt> quizAttempts = new ArrayList<>();

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM QuizAttempts WHERE quizId = ?");
            ps.setInt(1, quizId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                QuizAttempt quizAttempt = new QuizAttempt();
                quizAttempt.setAttemptId(rs.getInt("attemptId"));
                quizAttempt.setUserId(rs.getInt("userId"));
                quizAttempt.setQuizId(rs.getInt("quizId"));
                quizAttempt.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                quizAttempt.setScore(rs.getInt("score"));
                quizAttempt.setDurationAttempted(rs.getFloat("durationAttempted"));
                quizAttempts.add(quizAttempt);
            }
            rs.close();
            ps.close();
            connection.close();
        } catch (SQLException e) {
            throw new DAOException("Error getting quiz attempts by quiz ID", e);
        }

        return quizAttempts;
    }

    @Override
    public List<QuizAttempt> getQuizAttemptsByUserIdAndQuizId(int userId, int quizId) throws DAOException {
        Connection connection = dbConnection.getConnection();
        List<QuizAttempt> quizAttempts = new ArrayList<>();

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM QuizAttempts WHERE userId = ? AND quizId = ?");
            ps.setInt(1, userId);
            ps.setInt(2, quizId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                QuizAttempt quizAttempt = new QuizAttempt();
                quizAttempt.setAttemptId(rs.getInt("attemptId"));
                quizAttempt.setUserId(rs.getInt("userId"));
                quizAttempt.setQuizId(rs.getInt("quizId"));
                quizAttempt.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                quizAttempt.setScore(rs.getInt("score"));
                quizAttempt.setDurationAttempted(rs.getFloat("durationAttempted"));
                quizAttempts.add(quizAttempt);
            }
            rs.close();
            ps.close();
            connection.close();
        } catch (SQLException e) {
            throw new DAOException("Error getting quiz attempts by user ID and quiz ID", e);
        }

        return quizAttempts;
    }
    
    @Override
    public void updateQuizAttempt(QuizAttempt quizAttempt) throws DAOException {
        Connection connection = dbConnection.getConnection();

        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE QuizAttempts SET score = ?, durationAttempted = ? WHERE attemptId = ?");
            ps.setInt(1, quizAttempt.getScore());
            ps.setFloat(2, quizAttempt.getDurationAttempted());
            ps.setInt(3, quizAttempt.getAttemptId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new DAOException("Error updating quiz attempt", e);
        }
    }

    @Override
    public void deleteQuizAttempt(int attemptId) throws DAOException {
        Connection connection = dbConnection.getConnection();

        try {
            String deleteQuizAttemptQuestionsQuery = "DELETE FROM QuizAttemptQuestions WHERE attemptId = ?";
            PreparedStatement psQuizAttemptQuestions = connection.prepareStatement(deleteQuizAttemptQuestionsQuery);
            psQuizAttemptQuestions.setInt(1, attemptId);
            psQuizAttemptQuestions.executeUpdate();

            String deleteQuizAttemptQuery = "DELETE FROM QuizAttempts WHERE attemptId = ?";
            PreparedStatement psQuizAttempt = connection.prepareStatement(deleteQuizAttemptQuery);
            psQuizAttempt.setInt(1, attemptId);
            psQuizAttempt.executeUpdate();

            psQuizAttemptQuestions.close();
            psQuizAttempt.close();
        } catch (SQLException e) {
            throw new DAOException("Error deleting quiz attempt", e);
        }
    }
    
}