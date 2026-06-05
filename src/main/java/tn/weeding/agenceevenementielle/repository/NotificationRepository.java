package tn.weeding.agenceevenementielle.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.weeding.agenceevenementielle.entities.Notification;
import tn.weeding.agenceevenementielle.entities.Utilisateur;
import tn.weeding.agenceevenementielle.entities.enums.TypeNotification;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Récupérer toutes les notifications d'un utilisateur (triées par date DESC)
     */
    List<Notification> findByUtilisateurOrderByDateCreationDesc(Utilisateur utilisateur);

    /**
     * Récupérer les notifications non lues d'un utilisateur
     */
    List<Notification> findByUtilisateurAndLueFalseOrderByDateCreationDesc(Utilisateur utilisateur);

    /**
     * Compter les notifications non lues d'un utilisateur
     */
    Long countByUtilisateurAndLueFalse(Utilisateur utilisateur);



    /**
     * Marquer toutes les notifications comme lues
     */
    @Modifying
    @Query("UPDATE Notification n SET n.lue = true, n.dateLecture = :dateLecture " +
            "WHERE n.utilisateur = :utilisateur AND n.lue = false")
    void marquerToutesCommeLues(@Param("utilisateur") Utilisateur utilisateur,
                                @Param("dateLecture") LocalDateTime dateLecture);

    /**
     * Supprimer les anciennes notifications (nettoyage)
     */
    void deleteByDateCreationBefore(LocalDateTime date);

}