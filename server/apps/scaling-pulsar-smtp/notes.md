* En isolation, supprimer server/data/data-api/src/main/resources et rejouer tous les tests pour voir où ces fichiers manquent
* Loading
  * Extraire MailRepositoryFactory et l'implementer pour les différentes implems de MailRepository 
  * Remplacer l'implem de GuiceMailRepositoryLoader par celle du multibound
  * Créer les provides into set dans les modules des implem de mail repository (memory et cassandra)
