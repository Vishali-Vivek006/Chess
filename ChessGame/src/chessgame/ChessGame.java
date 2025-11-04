package chessgame;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import database.DatabaseConnection;

public class ChessGame extends JFrame {

    private BoardPanel boardPanel;
    private SidePanel sidePanel;
    private boolean whiteTurn = true;
    private Point selectedSquare = null;
    private Piece[][] board = new Piece[8][8];
    private List<Piece> whiteCaptured = new ArrayList<>(); 
    private List<Piece> blackCaptured = new ArrayList<>();
    private List<Point> possibleMoves = new ArrayList<>();
    private DatabaseConnection dbConnection;

    private ImageIcon Wsoldier, Wpawn, Wknight, Wbishop, Wqueen, WKing;
    private ImageIcon Blsoldier, Blpawn, BlKnight, Blbishop, BlQueen, BlKing;

    public ChessGame() {
        setTitle("Chess Master");
        setSize(900, 900);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        dbConnection = new DatabaseConnection();
        if (dbConnection.connect()) {
            System.out.println(" Database ready for saving games");
            dbConnection.displaySavedGames();
        } else {
            System.out.println(" Database connection failed - games won't be saved");
        }

        loadImages();

        initBoard();

        JPanel mainPanel = new JPanel(new BorderLayout(0,0));
        boardPanel = new BoardPanel();
        sidePanel = new SidePanel();
        
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        mainPanel.add(sidePanel, BorderLayout.EAST);
        
        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadImages() {
        try {
            Wsoldier = new ImageIcon("src/images/Wsoldier.png");
            Wpawn    = new ImageIcon("src/images/Wpawn.png");
            Wknight  = new ImageIcon("src/images/Wknight.png");
            Wbishop  = new ImageIcon("src/images/Wbishop.png");
            Wqueen   = new ImageIcon("src/images/Wqueen.png");
            WKing    = new ImageIcon("src/images/WKing.png");

            Blsoldier = new ImageIcon("src/images/Blsoldier.png");
            Blpawn    = new ImageIcon("src/images/Blpawn.png");
            BlKnight  = new ImageIcon("src/images/BlKnight.png");
            Blbishop  = new ImageIcon("src/images/Blbishop.png");
            BlQueen   = new ImageIcon("src/images/Blqueen.png");
            BlKing    = new ImageIcon("src/images/Blking.png");
        } catch (Exception e) {
            System.out.println("Error loading images: " + e.getMessage());
           
        }
    }

   
    private void initBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = null;
            }
        }

        for (int i = 0; i < 8; i++) {
            board[1][i] = new Piece("P", false, Blsoldier);
            board[6][i] = new Piece("P", true, Wsoldier);
        }
        board[0][0] = new Piece("R", false, Blpawn);
        board[0][7] = new Piece("R", false, Blpawn);
        board[7][0] = new Piece("R", true, Wpawn);
        board[7][7] = new Piece("R", true, Wpawn);

        board[0][1] = new Piece("N", false, BlKnight);
        board[0][6] = new Piece("N", false, BlKnight);
        board[7][1] = new Piece("N", true, Wknight);
        board[7][6] = new Piece("N", true, Wknight);

        board[0][2] = new Piece("B", false, Blbishop);
        board[0][5] = new Piece("B", false, Blbishop);
        board[7][2] = new Piece("B", true, Wbishop);
        board[7][5] = new Piece("B", true, Wbishop);

        board[0][3] = new Piece("Q", false, BlQueen);
        board[7][3] = new Piece("Q", true, Wqueen);

        board[0][4] = new Piece("K", false, BlKing);
        board[7][4] = new Piece("K", true, WKing);
    }

    private class BoardPanel extends JPanel {
        private final int TILE_SIZE = 75;

        public BoardPanel() {
            setPreferredSize(new Dimension(8 * TILE_SIZE, 8 * TILE_SIZE));
            setBorder(BorderFactory.createLineBorder(new Color(120, 80, 40), 3));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int row = e.getY() / TILE_SIZE;
                    int col = e.getX() / TILE_SIZE;
                    if (row < 0 || row > 7 || col < 0 || col > 7) return;

                    if (selectedSquare == null) {
                        if (board[row][col] != null && board[row][col].isWhite == whiteTurn) {
                            selectedSquare = new Point(row, col);
                            possibleMoves = generateLegalMoves(selectedSquare.x, selectedSquare.y);
                            repaint();
                        }
                    } else {
                        boolean isPossibleMove = false;
                        for (Point move : possibleMoves) {
                            if (move.x == row && move.y == col) {
                                isPossibleMove = true;
                                break;
                            }
                        }
                        
                        if (isPossibleMove && tryMove(selectedSquare.x, selectedSquare.y, row, col)) {
                            whiteTurn = !whiteTurn;
                            sidePanel.repaint();
                            repaint();
                            selectedSquare = null;
                            possibleMoves.clear();

                            if (isCheckmate(false)) {
                                gameOver("White Wins by Checkmate!");
                                return;
                            }

                            if (!whiteTurn) {
                                new Thread(() -> {
                                    AIMove();
                                    if (isCheckmate(true)) {
                                        SwingUtilities.invokeLater(() -> 
                                            gameOver("Black Wins by Checkmate!")
                                        );
                                    }
                                    whiteTurn = !whiteTurn;
                                    sidePanel.repaint();
                                    repaint();
                                }).start();
                            }
                        } else {
                            if (board[row][col] != null && board[row][col].isWhite == whiteTurn) {
                                selectedSquare = new Point(row, col);
                                possibleMoves = generateLegalMoves(selectedSquare.x, selectedSquare.y);
                            } else {
                                selectedSquare = null;
                                possibleMoves.clear();
                            }
                            repaint();
                        }
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final int TILE = TILE_SIZE;

            Color lightWood = new Color(240, 217, 181);
            Color darkWood  = new Color(181, 136, 99);

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    boolean light = (r + c) % 2 == 0;
                    g.setColor(light ? lightWood : darkWood);
                    g.fillRect(c * TILE, r * TILE, TILE, TILE);

                    if (selectedSquare != null && selectedSquare.x == r && selectedSquare.y == c) {
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setColor(new Color(255, 215, 0));
                        g2d.setStroke(new BasicStroke(4));
                        g2d.drawRect(c * TILE + 2, r * TILE + 2, TILE - 4, TILE - 4);
                    }

                    for (Point move : possibleMoves) {
                        if (move.x == r && move.y == c) {
                            Graphics2D g2d = (Graphics2D) g;
                            g2d.setColor(new Color(50, 205, 50));
                            g2d.setStroke(new BasicStroke(3));
                            g2d.drawRect(c * TILE + 2, r * TILE + 2, TILE - 4, TILE - 4);
                        }
                    }

                    Piece p = board[r][c];
                    if (p != null && p.image != null) {
                        g.drawImage(p.image.getImage(), c * TILE, r * TILE, TILE, TILE, this);
                    }
                }
            }

            Point wK = findKing(true);
            Point bK = findKing(false);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(3));
            if (wK != null && isKingInCheck(true)) {
                g2d.setColor(new Color(255, 0, 0));
                g2d.drawRect(wK.y * TILE + 2, wK.x * TILE + 2, TILE - 4, TILE - 4);
            }
            if (bK != null && isKingInCheck(false)) {
                g2d.setColor(new Color(255, 0, 0));
                g2d.drawRect(bK.y * TILE + 2, bK.x * TILE + 2, TILE - 4, TILE - 4);
            }

            g.setColor(new Color(80, 50, 20));
            g.setFont(new Font("Arial", Font.BOLD, 12));
            for (int i = 0; i < 8; i++) {
                g.drawString(String.valueOf((char)('a' + i)), i * TILE + TILE/2 - 5, TILE * 8 - 5);
                g.drawString(String.valueOf(8 - i), 5, i * TILE + TILE/2 + 5);
            }
        }
    }

    private class SidePanel extends JPanel {
        private final int PANEL_WIDTH = 250;
        
        public SidePanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, 8 * 75));
            setBackground(new Color(245, 245, 220));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(new Color(80, 50, 20));
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("CHESS MASTER", 20, 30);
            
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Active Player:", 20, 70);
            
            if (whiteTurn) {
                g2d.setColor(new Color(240, 217, 181));
                g2d.fillRect(20, 80, 160, 40);
                g2d.setColor(new Color(80, 50, 20));
                g2d.drawRect(20, 80, 160, 40);
                g2d.drawString("WHITE", 75, 105);
            } else {
                g2d.setColor(new Color(181, 136, 99));
                g2d.fillRect(20, 80, 160, 40);
                g2d.setColor(new Color(80, 50, 20));
                g2d.drawRect(20, 80, 160, 40);
                g2d.drawString("BLACK", 75, 105);
            }
            
            g2d.setColor(new Color(80, 50, 20));
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("PLAYER Captured:", 20, 150);
            
            int yPos = 170;
            for (int i = 0; i < whiteCaptured.size(); i++) {
                Piece p = whiteCaptured.get(i);
                if (p.image != null) {
                    g2d.drawImage(p.image.getImage(), 20 + (i % 4) * 35, yPos + (i / 4) * 35, 30, 30, this);
                }
            }
            
            g2d.drawString("BOT Captured:", 20, 350);
            yPos = 370;
            for (int i = 0; i < blackCaptured.size(); i++) {
                Piece p = blackCaptured.get(i);
                if (p.image != null) {
                    g2d.drawImage(p.image.getImage(), 20 + (i % 4) * 35, yPos + (i / 4) * 35, 30, 30, this);
                }
            }
            
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Game Status:", 20, 560);
            
            if (isCheckmate(true)) {
                g2d.setColor(Color.RED);
                g2d.drawString("WHITE IN CHECKMATE", 20, 585);
            } else if (isCheckmate(false)) {
                g2d.setColor(Color.RED);
                g2d.drawString("BLACK IN CHECKMATE", 20, 585);
            } else if (isKingInCheck(true)) {
                g2d.setColor(Color.ORANGE);
                g2d.drawString("WHITE IN CHECK", 20, 585);
            } else if (isKingInCheck(false)) {
                g2d.setColor(Color.ORANGE);
                g2d.drawString("BLACK IN CHECK", 20, 585);
            } else {
                g2d.setColor(Color.GREEN);
                g2d.drawString("NORMAL", 20, 585);
            }
        }
    }

    private void saveGameToDatabase(String playerName, String result) {
        if (dbConnection == null || !dbConnection.isConnected()) {
            JOptionPane.showMessageDialog(this, 
                "No database connection. Game cannot be saved.",
                "Database Error", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder whiteCapturedStr = new StringBuilder();
        for (Piece p : whiteCaptured) {
            whiteCapturedStr.append(p.type).append(",");
        }
        
        StringBuilder blackCapturedStr = new StringBuilder();
        for (Piece p : blackCaptured) {
            blackCapturedStr.append(p.type).append(",");
        }

        boolean success = dbConnection.saveGame(
            playerName != null ? playerName : "Anonymous",
            result,
            whiteCapturedStr.toString(),
            blackCapturedStr.toString()
        );

        if (success) {
            JOptionPane.showMessageDialog(this, 
                "Game saved to database successfully!\n" +
                "Player: " + (playerName != null ? playerName : "Anonymous") +
                "\nResult: " + result,
                "Game Saved", 
                JOptionPane.INFORMATION_MESSAGE);
            
            dbConnection.displaySavedGames();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to save game to database. Please check console for errors.",
                "Save Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void gameOver(String message) {
        String playerName = JOptionPane.showInputDialog(this, 
            "Game Over!\n" + message + "\n\nEnter your name to save the game:",
            "Game Finished", 
            JOptionPane.QUESTION_MESSAGE);
        
        saveGameToDatabase(playerName, message);
        
        int choice = JOptionPane.showConfirmDialog(this, 
            message + "\n\nWould you like to play again?", 
            "Game Over", 
            JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            resetGame();
        } else {
            if (dbConnection != null) {
                dbConnection.close();
            }
            System.exit(0);
        }
    }

    private void resetGame() {
        whiteTurn = true;
        whiteCaptured.clear();
        blackCaptured.clear();
        selectedSquare = null;
        possibleMoves.clear();
        initBoard();
        repaint();
        sidePanel.repaint();
    }

    private boolean tryMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = board[fromRow][fromCol];
        if (piece == null) return false;

        boolean allowedDest = false;
        for (Point p : possibleMoves) {
            if (p.x == toRow && p.y == toCol) {
                allowedDest = true;
                break;
            }
        }
        if (!allowedDest) return false;

        Piece captured = board[toRow][toCol];
        
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = null;

        boolean kingStillInCheck = isKingInCheck(piece.isWhite);

        if (kingStillInCheck) {
            board[fromRow][fromCol] = piece;
            board[toRow][toCol] = captured;
            return false;
        }

        if (captured != null) {
            if (piece.isWhite) {
                whiteCaptured.add(captured);
            } else {
                blackCaptured.add(captured);
            }
            sidePanel.repaint();
        }

        return true;
    }

    @Override
    public void dispose() {
        if (dbConnection != null) {
            dbConnection.close();
        }
        super.dispose();
    }


    private List<Point> generateLegalMoves(int row, int col) {
        Piece p = board[row][col];
        List<Point> moves = new ArrayList<>();
        if (p == null) return moves;

        List<Point> allMoves = generateMoves(row, col);
        
        for (Point move : allMoves) {
            Piece captured = board[move.x][move.y];
            board[move.x][move.y] = p;
            board[row][col] = null;
            
            boolean inCheck = isKingInCheck(p.isWhite);
            
            board[row][col] = p;
            board[move.x][move.y] = captured;
            
            if (!inCheck) {
                moves.add(move);
            }
        }
        
        return moves;
    }

    private List<Point> generateMoves(int row, int col) {
        Piece p = board[row][col];
        List<Point> moves = new ArrayList<>();
        if (p == null) return moves;

        switch (p.type) {
            case "P":
                int dir = p.isWhite ? -1 : 1;
                int startRow = p.isWhite ? 6 : 1;
                
                if (inBounds(row + dir, col) && board[row + dir][col] == null) {
                    moves.add(new Point(row + dir, col));
                    
                    if (row == startRow && inBounds(row + 2 * dir, col) && board[row + 2 * dir][col] == null) {
                        moves.add(new Point(row + 2 * dir, col));
                    }
                }
                
                if (inBounds(row + dir, col - 1) && board[row + dir][col - 1] != null && 
                    board[row + dir][col - 1].isWhite != p.isWhite) {
                    moves.add(new Point(row + dir, col - 1));
                }
                if (inBounds(row + dir, col + 1) && board[row + dir][col + 1] != null && 
                    board[row + dir][col + 1].isWhite != p.isWhite) {
                    moves.add(new Point(row + dir, col + 1));
                }
                break;
                
            case "R":
                moves.addAll(slideMoves(row, col, p.isWhite, new int[][]{{1,0},{-1,0},{0,1},{0,-1}}));
                break;
                
            case "N":
                int[][] knightMoves = {{-2,-1}, {-2,1}, {-1,-2}, {-1,2}, {1,-2}, {1,2}, {2,-1}, {2,1}};
                for (int[] m : knightMoves) {
                    int r2 = row + m[0], c2 = col + m[1];
                    if (inBounds(r2, c2) && (board[r2][c2] == null || board[r2][c2].isWhite != p.isWhite)) {
                        moves.add(new Point(r2, c2));
                    }
                }
                break;
                
            case "B":
                moves.addAll(slideMoves(row, col, p.isWhite, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}));
                break;
                
            case "Q":
                moves.addAll(slideMoves(row, col, p.isWhite, new int[][]{
                    {1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}
                }));
                break;
                
            case "K":
                int[][] kingMoves = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
                for (int[] m : kingMoves) {
                    int r2 = row + m[0], c2 = col + m[1];
                    if (inBounds(r2, c2) && (board[r2][c2] == null || board[r2][c2].isWhite != p.isWhite)) {
                        moves.add(new Point(r2, c2));
                    }
                }
                break;
        }

        return moves;
    }

    private List<Point> slideMoves(int row, int col, boolean isWhite, int[][] directions) {
        List<Point> moves = new ArrayList<>();
        for (int[] d : directions) {
            int r = row + d[0];
            int c = col + d[1];
            while (inBounds(r, c)) {
                if (board[r][c] == null) {
                    moves.add(new Point(r, c));
                } else {
                    if (board[r][c].isWhite != isWhite) {
                        moves.add(new Point(r, c));
                    }
                    break;
                }
                r += d[0];
                c += d[1];
            }
        }
        return moves;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    private void AIMove() {
        if (isCheckmate(false)) return;

        List<Move> allMoves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && !p.isWhite) {
                    List<Point> moves = generateLegalMoves(r, c);
                    for (Point m : moves) {
                        allMoves.add(new Move(r, c, m.x, m.y));
                    }
                }
            }
        }

        if (allMoves.isEmpty()) return;

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move move : allMoves) {
            Piece captured = board[move.toRow][move.toCol];
            int score = 0;
            
            if (captured != null && captured.isWhite) {
                score += getPieceValue(captured.type) * 100;
            }

            int centerBonus = (4 - Math.abs(3 - move.toRow)) + (4 - Math.abs(3 - move.toCol));
            score += centerBonus;

            score += getPieceValue(board[move.fromRow][move.fromCol].type);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        if (bestMove != null) {
            Piece captured = board[bestMove.toRow][bestMove.toCol];
            if (captured != null) {
                blackCaptured.add(captured);
                sidePanel.repaint();
            }
            
            board[bestMove.toRow][bestMove.toCol] = board[bestMove.fromRow][bestMove.fromCol];
            board[bestMove.fromRow][bestMove.fromCol] = null;
        }
    }

    private int getPieceValue(String type) {
        switch (type) {
            case "P": return 1;
            case "N": return 3;
            case "B": return 3;
            case "R": return 5;
            case "Q": return 9;
            case "K": return 100;
            default: return 0;
        }
    }

    private int evaluateBoardForWhite() {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null) {
                    int val = getPieceValue(p.type);
                    score += (p.isWhite ? val : -val);
                }
            }
        }
        return score;
    }

    private Point findKing(boolean isWhite) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.type.equals("K") && p.isWhite == isWhite) {
                    return new Point(r, c);
                }
            }
        }
        return null;
    }

    private boolean isUnderAttack(int row, int col, boolean byWhite) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.isWhite == byWhite) {
                    List<Point> moves = generateMoves(r, c);
                    for (Point m : moves) {
                        if (m.x == row && m.y == col) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isKingInCheck(boolean isWhite) {
        Point kingPos = findKing(isWhite);
        if (kingPos == null) return false;
        return isUnderAttack(kingPos.x, kingPos.y, !isWhite);
    }

    private boolean hasAnyLegalMove(boolean isWhite) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.isWhite == isWhite) {
                    List<Point> moves = generateLegalMoves(r, c);
                    if (!moves.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isCheckmate(boolean isWhite) {
        if (!isKingInCheck(isWhite)) return false;
        return !hasAnyLegalMove(isWhite);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGame::new);
    }

    private static class Piece {
        String type;
        boolean isWhite;
        ImageIcon image;

        public Piece(String type, boolean isWhite, ImageIcon image) {
            this.type = type;
            this.isWhite = isWhite;
            this.image = image;
        }
    }

    private static class Move {
        int fromRow, fromCol, toRow, toCol;

        public Move(int fr, int fc, int tr, int tc) {
            fromRow = fr;
            fromCol = fc;
            toRow = tr;
            toCol = tc;
        }
    }
}