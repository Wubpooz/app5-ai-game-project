package game;

import interfaces.IJoueur;


public class EscampeAIPlayer implements IJoueur {
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
  // @param mycolour La couleur dans laquelle vous allez jouer (-1=BLANC, 1=NOIR)
  public void initJoueur(int mycolour) {
    this.role = (mycolour == PlayerColor.WHITE.getValue()) ? PlayerColor.WHITE : PlayerColor.BLACK;
    this.opponentRole = (this.role == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
  }

  // Doit retourner l'argument passé par la fonction ci-dessus (constantes BLANC ou NOIR)
  public int getNumJoueur() {
    return this.role.getValue();
  }

  /**
   * C'est ici que vous devez faire appel à votre IA pour trouver le meilleur coup à jouer sur le
   * plateau courant.
   * 
   * @return une chaine décrivant le mouvement. Cette chaine doit être décrite exactement comme
   *         sur l'exemple : String msg = "" + positionInitiale + "-" +positionFinale + ""; ou "PASSE";
   *          Chaque position contient une lettre et un num?ro, par exemple:A1,B2 (coup "A1-B2")
   */
  public String choixMouvement() {
    return ai.bestMove(board, this.role).toString();
  }


  /**
   * On suppose que l'arbitre a vérifié que le mouvement ennemi était bien légal. Il vous informe
   * du mouvement ennemi. A vous de répercuter ce mouvement dans vos structures. Comme par exemple
   * éliminer les pions que ennemi vient de vous prendre par ce mouvement. Il n'est pas nécessaire
   * de réfléchir déjà à votre prochain coup à jouer : pour cela l'arbitre appelera ensuite
   * choixMouvement().
   * 
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
    return "Mat&Raph AIPlayer";
  }
}
