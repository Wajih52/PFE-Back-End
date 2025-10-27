package tn.weeding.agenceevenementielle.exceptions;

/**
 * Exception personnalisée pour les erreurs liées aux produits
 */
public class ProduitException extends RuntimeException {

    public ProduitException(String message) {
        super(message);
    }

    public ProduitException(String message, Throwable cause) {
        super(message, cause);
    }

    // Exceptions spécifiques

    public static class ProduitNotFoundException extends ProduitException {
        public ProduitNotFoundException(Long idProduit) {
            super("Produit introuvable avec l'ID : " + idProduit);
        }

        public ProduitNotFoundException(String codeProduit) {
            super("Produit introuvable avec le code : " + codeProduit);
        }
    }

    public static class StockInsuffisantException extends ProduitException {
        public StockInsuffisantException(String nomProduit, Integer quantiteRequise, Integer quantiteDisponible) {
            super(String.format("Stock insuffisant pour le produit '%s'. Demandé : %d, Disponible : %d",
                    nomProduit, quantiteRequise, quantiteDisponible));
        }
    }

    public static class QuantiteInvalideException extends ProduitException {
        public QuantiteInvalideException(String message) {
            super(message);
        }
    }

    public static class CodeProduitExistantException extends ProduitException {
        public CodeProduitExistantException(String codeProduit) {
            super("Un produit avec le code " + codeProduit + " existe déjà");
        }
    }

    public static class ProduitAvecReservationsException extends ProduitException {
        public ProduitAvecReservationsException(Long idProduit) {
            super("Le produit avec l'ID " + idProduit + " ne peut pas être supprimé car il a des réservations actives");
        }
    }
}