package com.neumatica.embudo.whatsap.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.interfaces.DailyContactActivityProjection;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID>{
	
	Page<Contact> findAllByOrderByCreatedAtDesc(Pageable pageable);
	
	@Query("""
			select c from Contact c
			""")
	List<Contact> findAllEmails();

	Optional<Contact> findByPhone(String phone);
	
	Optional<Contact> findByMetaUserId(String metaUserId);
	
	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
	        LocalDateTime from,
	        LocalDateTime to
	);

	long countByLastInteractionGreaterThanEqualAndLastInteractionLessThan(
	        LocalDateTime from,
	        LocalDateTime to
	);
	
	@Query("""
		    SELECT COUNT(c)
		    FROM Contact c
		    WHERE c.phone IS NOT NULL
		      AND TRIM(c.phone) <> ''
		""")
		long countContactsWithPhone();


		@Query("""
		    SELECT COUNT(c)
		    FROM Contact c
		    WHERE c.name IS NOT NULL
		      AND TRIM(c.name) <> ''
		""")
		long countContactsWithName();


		@Query("""
		    SELECT COUNT(c)
		    FROM Contact c
		    WHERE c.email IS NOT NULL
		      AND TRIM(c.email) <> ''
		""")
		long countContactsWithEmail();


		@Query("""
		    SELECT COUNT(c)
		    FROM Contact c
		    WHERE c.company IS NOT NULL
		      AND TRIM(c.company) <> ''
		""")
		long countContactsWithCompany();
		
		@Query(
			    value = """
			        SELECT
			            CAST(c.created_at AS DATE) AS date,
			            COUNT(*) AS total
			        FROM contact c
			        WHERE c.created_at >= :from
			          AND c.created_at < :to
			        GROUP BY
			            CAST(c.created_at AS DATE)
			        ORDER BY
			            CAST(c.created_at AS DATE)
			        """,
			    nativeQuery = true
			)
			List<DailyContactActivityProjection> findDailyContactActivity(
			    @Param("from") LocalDateTime from,
			    @Param("to") LocalDateTime to
			);
}
