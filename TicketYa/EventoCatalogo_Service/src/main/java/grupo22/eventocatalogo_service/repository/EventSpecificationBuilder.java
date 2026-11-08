package grupo22.eventocatalogo_service.repository;

import grupo22.eventocatalogo_service.domain.Event;
import grupo22.eventocatalogo_service.domain.EventSectorPrice;
import grupo22.eventocatalogo_service.domain.EventStatus;
import grupo22.eventocatalogo_service.domain.Participant;
import grupo22.eventocatalogo_service.DTO.request.EventSearchFilterDTO;
import grupo22.eventocatalogo_service.DTO.request.EventSortOption;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EventSpecificationBuilder {

    private EventSpecificationBuilder() {}

    public static Specification<Event> buildSpecification(EventSearchFilterDTO filter, EventSortOption sortOption) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Regla de Negocio: Solo eventos PUBLICADO o AGOTADO en el catálogo público
            predicates.add(root.get("status").in(EventStatus.PUBLICADO, EventStatus.AGOTADO));

            if (filter != null) {
                // Filtro por Tipo
                if (filter.getType() != null && !filter.getType().isBlank()) {
                    predicates.add(cb.equal(cb.lower(root.get("eventType")), filter.getType().trim().toLowerCase()));
                }

                // Filtro por Organizador
                if (filter.getOrganizerId() != null) {
                    predicates.add(cb.equal(root.get("organizerId"), filter.getOrganizerId()));
                }

                // Filtro por Participante
                if (filter.getParticipantId() != null) {
                    Join<Event, Participant> participantJoin = root.join("participants", JoinType.INNER);
                    predicates.add(cb.equal(participantJoin.get("participantId"), filter.getParticipantId()));
                }

                // Filtro por Rango de Fechas
                if (filter.getFromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("startDateTime"), filter.getFromDate()));
                }
                if (filter.getToDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("startDateTime"), filter.getToDate()));
                }
            }

            // 2. Aplicar Criterios de Ordenamiento
            if (sortOption != null) {
                switch (sortOption) {
                    case DATE_ASC -> query.orderBy(cb.asc(root.get("startDateTime")));
                    case DATE_DESC -> query.orderBy(cb.desc(root.get("startDateTime")));
                    case PRICE_ASC, PRICE_DESC -> {
                        // Subquery o Join con EventSectorPrice para ordenar por el precio mínimo del evento
                        Subquery<Double> priceSubquery = query.subquery(Double.class);
                        Root<EventSectorPrice> priceRoot = priceSubquery.from(EventSectorPrice.class);
                        priceSubquery.select(cb.min(priceRoot.get("price").as(Double.class)))
                                .where(cb.equal(priceRoot.get("event"), root));

                        if (sortOption == EventSortOption.PRICE_ASC) {
                            query.orderBy(cb.asc(priceSubquery));
                        } else {
                            query.orderBy(cb.desc(priceSubquery));
                        }
                    }
                }
            } else {
                // Orden por defecto: Fecha más cercana
                query.orderBy(cb.asc(root.get("startDateTime")));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}