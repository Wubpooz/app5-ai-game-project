package game;

import interfaces.IJoueur;


public class EscampeAIPlayer implements IJoueur {
  private static final String TEAM_NAME = "Mat&Raph AIPlayer";

	private PlayerColor role;
  private EscampeBoard board;
	private algorithms.GameAlgorithm<EscampeMove,PlayerColor,EscampeBoard> ai;

  // Helpful variables
  private PlayerColor opponentRole;


  // Constructors
	public EscampeAIPlayer(algorithms.GameAlgorithm<EscampeMove,PlayerColor,EscampeBoard> alg) {
		this.ai = alg;
	}

  // Interface Methods
  /**
   * @param mycolour La couleur dans laquelle vous allez jouer (-1=BLANC, 1=NOIR)
  */
  public void initJoueur(int mycolour) {
    this.role = (mycolour == PlayerColor.WHITE.getValue()) ? PlayerColor.WHITE : PlayerColor.BLACK;
    this.opponentRole = (this.role == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
    
    this.board = new EscampeBoard();
    this.board.initializeBoard();

    // this.ai.setPlayerColor(this.role);
    // this.ai.setOpponentColor(this.opponentRole);
    // this.ai.init(this.board);
  }

  // Doit retourner l'argument passé par la fonction ci-dessus (constantes BLANC ou NOIR)
  public int getNumJoueur() {
    return this.role.getValue();
  }

  public String choixMouvement() {
    return ai.bestMove(board, this.role).toString(); // TODO String msg = "" + positionInitiale + "-" +positionFinale + ""; ou "PASSE"; avec position.. = A1,B2
  }


  /**
   * @param coup
   * 			une chaine décrivant le mouvement:  par exemple: "A1-B2"
   */
  public void mouvementEnnemi(String coup) {
    EscampeMove move = new EscampeMove(coup);
    board.play(move, this.opponentRole);
  }


  public void declareLeVainqueur(int colour) {
    //TODO
    if (colour == this.getNumJoueur()) {
      System.out.println("I win!");
    } else {
    System.out.println("I lose...");
    }
  }

  public String binoName() {
    return TEAM_NAME;
  }
}
