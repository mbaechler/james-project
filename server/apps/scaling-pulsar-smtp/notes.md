WIN : no more cassandra, no more ES !

* PR Mailrepository Loading
  * Extraire MailRepositoryFactory et l'implementer pour les différentes implems de MailRepository 
  * Remplacer l'implem de GuiceMailRepositoryLoader par celle du multibound
  * Créer les provides into set dans les modules des implem de mail repository (memory et cassandra)
* Clean dependencies in pom (scaling & mpt)
