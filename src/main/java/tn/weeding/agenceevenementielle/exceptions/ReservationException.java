package tn.weeding.agenceevenementielle.exceptions;

/**
 * Exception personnalisée pour les erreurs liées aux réservations
 * Sprint 4 - Gestion des réservations
 */
public class ReservationException extends RuntimeException {

    public ReservationException(String message) {
        super(message);
    }

    public ReservationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception levée quand une réservation est introuvable
     */
    public static class ReservationNotFoundException extends ReservationException {
        public ReservationNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception levée quand le stock est insuffisant
     */
    public static class StockInsuffisantException extends ReservationException {
        private final Long idProduit;
        private final Integer quantiteDemandee;
        private final Integer quantiteDisponible;

        public StockInsuffisantException(Long idProduit, Integer quantiteDemandee, Integer quantiteDisponible) {
            super(String.format("Stock insuffisant pour le produit ID %d. Demandé: %d, Disponible: %d",
                    idProduit, quantiteDemandee, quantiteDisponible));
            this.idProduit = idProduit;
            this.quantiteDemandee = quantiteDemandee;
            this.quantiteDisponible = quantiteDisponible;
        }

        public Long getIdProduit() {
            return idProduit;
        }

        public Integer getQuantiteDemandee() {
            return quantiteDemandee;
        }

        public Integer getQuantiteDisponible() {
            return quantiteDisponible;
        }
    }

    /**
     * Exception levée quand les dates sont invalides
     */
    public static class DateInvalideException extends ReservationException {
        public DateInvalideException(String message) {
            super(message);
        }
    }

    /**
     * Exception levée quand une modification est impossible
     */
    public static class ModificationImpossibleException extends ReservationException {
        public ModificationImpossibleException(String message) {
            super(message);
        }
    }

    /**
     * Exception levée quand une annulation est impossible
     */
    public static class AnnulationImpossibleException extends ReservationException {
        public AnnulationImpossibleException(String message) {
            super(message);
        }
    }
    /**
     * Exception levée quand un produit n'est plus disponible lors de la validation d'un devis
     *
     * Utilisée dans la logique SOFT BOOKING:
     * - Quand un client tente de valider un devis mais que le stock a été réservé entre-temps
     * - Quand un admin tente de valider un devis avec un stock devenu indisponible
     */
    public static class StockIndisponibleException extends RuntimeException {

        public StockIndisponibleException(String message) {
            super(message);
        }

        public StockIndisponibleException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}