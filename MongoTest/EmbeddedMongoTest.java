import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest  // starts embedded Mongo automatically
class EmbeddedMongoTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setup() throws Exception {
        // Load multiple collections before each test
        MongoTestUtils.loadJsonIntoCollection(mongoTemplate, "data/transactions.json", "transactions");
        MongoTestUtils.loadJsonIntoCollection(mongoTemplate, "data/orders.json", "orders");
    }

    @Test
    void testFindSuccessTransactions() {
        Query q = Query.query(Criteria.where("status").is("SUCCESS"));
        List<Document> result = mongoTemplate.find(q, Document.class, "transactions");

        assertThat(result).hasSize(2);
    }

    @Test
    void testFindOrdersWithPriceGt30000() {
        Query q = Query.query(Criteria.where("price").gt(30000));
        List<Document> result = mongoTemplate.find(q, Document.class, "orders");

        assertThat(result).extracting(d -> d.get("item"))
                          .containsExactly("Laptop");
    }
}
