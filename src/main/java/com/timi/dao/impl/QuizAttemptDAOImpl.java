package com.timi.dao.impl;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Type;
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
import com.timi.service.AuditingService;
import com.timi.service.impl.AuditingServiceImpl;

public class QuizAttemptDAOImpl implements QuizAttemptDAO {
    
    private DatabaseConnection dbConnection;
    private AuditingService auditingService;

    public QuizAttemptDAOImpl() {
        dbConnection = DatabaseConnection.getInstance();
        auditingService = AuditingServiceImpl.getInstance();
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

            if (quizAttempt.getAttemptId() == 0) {
                PreparedStatement psGetId = connection.prepareStatement("SELECT LAST_INSERT_ID()");
                ResultSet rs = psGetId.executeQuery();
                if (rs.next()) {
                    quizAttempt.setAttemptId(rs.getInt(1));
                }
                rs.close();
                psGetId.close();
            }

            for (Question question : quizAttempt.getQuestionsAttempted()) {
                PreparedStatement psQuestions = connection.prepareStatement("INSERT INTO QuizAttemptQuestions (attemptId, questionId, selectedOptionIndex) VALUES (?, ?, ?)");
                psQuestions.setInt(1, quizAttempt.getAttemptId());
                psQuestions.setInt(2, question.getQuestionId());
                psQuestions.setInt(3, question.getSelectedOptionIndex());
                psQuestions.executeUpdate();
                psQuestions.close();
            }
            
            auditingService.logCurrentAction();
        } catch (SQLException e) {
            e.printStackTrace();
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
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                quizAttempt = new QuizAttempt();
                quizAttempt.setAttemptId(rs.getInt("attemptId"));
                quizAttempt.setUserId(rs.getInt("userId"));
                quizAttempt.setQuizId(rs.getInt("quizId"));
                quizAttempt.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                quizAttempt.setScore(rs.getInt("score"));
                quizAttempt.setDurationAttempted(rs.getFloat("durationAttempted"));
                
                List<Question> questionsAttempted = getQuestionsByAttemptId(quizAttempt.getAttemptId());
                quizAttempt.setQuestionsAttempted(questionsAttempted);
            }
            rs.close();
            ps.close();
            auditingService.logCurrentAction();
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
                
                // Fetch questions attempted for this quiz attempt
                List<Question> questionsAttempted = getQuestionsByAttemptId(quizAttempt.getAttemptId());
                quizAttempt.setQuestionsAttempted(questionsAttempted);
                
                quizAttempts.add(quizAttempt);
            }
            rs.close();
            ps.close();
            auditingService.logCurrentAction();
        } catch (SQLException e) {
            throw new DAOException("Error getting all quiz attempts", e);
        }
    
        return quizAttempts;
    }

    @Override
    public List<Question> getQuestionsByAttemptId(int attemptId) throws DAOException {
        Connection connection = dbConnection.getConnection();
        List<Question> questions = new ArrayList<>();

        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM QuizAttemptQuestions WHERE attemptId = ?");
            ps.setInt(1, attemptId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int questionId = rs.getInt("questionId");
                QuestionDAOImpl questionDAO = new QuestionDAOImpl();
                Question question = questionDAO.getQuestionById(questionId);
                question.setSelectedOptionIndex(rs.getInt("selectedOptionIndex"));
                
                // get the other information of the question from the Questions table
                PreparedStatement psQuestion = connection.prepareStatement("SELECT * FROM Questions WHERE questionId = ?");
                psQuestion.setInt(1, questionId);
                ResultSet rsQuestion = psQuestion.executeQuery();
                if (rsQuestion.next()) {
                    question.setContent(rsQuestion.getString("content"));
                    String optionsJson = rsQuestion.getString("options");
                    Type listType = new TypeToken<List<String>>(){}.getType();
                    List<String> options = new Gson().fromJson(optionsJson, listType);
                    question.setOptions(options);
                    question.setCorrectOptionIndex(rsQuestion.getInt("correctOptionIndex"));
                    question.setQuizId(rsQuestion.getInt("quizId"));
                }
                rsQuestion.close();
                psQuestion.close();
                questions.add(question);
            }
            rs.close();
            ps.close();
            auditingService.logCurrentAction();
        } catch (SQLException e) {
            throw new DAOException("Error getting questions by attempt ID", e);
        }

        return questions;
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

                List<Question> questionsAttempted = getQuestionsByAttemptId(quizAttempt.getAttemptId());
                quizAttempt.setQuestionsAttempted(questionsAttempted);
            }
            rs.close();
            ps.close();
            auditingService.logCurrentAction();
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

                List<Question> questionsAttempted = getQuestionsByAttemptId(quizAttempt.getAttemptId());
                quizAttempt.setQuestionsAttempted(questionsAttempted);
            }
            rs.close();
            ps.close();
            auditingService.logCurrentAction();
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

                List<Question> questionsAttempted = getQuestionsByAttemptId(quizAttempt.getAttemptId());
                quizAttempt.setQuestionsAttempted(questionsAttempted);
            }
            rs.close();
            ps.close();
            auditingService.logCurrentAction();
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
            auditingService.logCurrentAction();
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
            auditingService.logCurrentAction();
        } catch (SQLException e) {
            throw new DAOException("Error deleting quiz attempt", e);
        }
    }
    
}
