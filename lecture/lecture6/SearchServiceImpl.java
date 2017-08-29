package io.test.www.service.core;

import io.test.www.dto.search.QueryParam;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;
import org.apache.commons.text.*;


import java.net.InetAddress;

import static org.elasticsearch.index.query.QueryBuilders.geoDistanceQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;


/*
******쿼리 정리*******

1) MatchAllQuery

QueryBuilder qb = matchAllQuery();

2) Match Query
QueryBuilder qb = matchQuery(
    "name",
    "kimchy elasticsearch"
);
필드,텍스트

3) MultiMatchQuery
QueryBuilder qb = multiMatchQuery(
    "kimchy elasticsearch",
    "user", "message"
);
텍스트 , 필드

4) Query String Query
QueryBuilder qb = queryStringQuery("+kimchy -elasticsearch");
루씬 문법 사용 Boolean 식 적용

5) Term Query
QueryBuilder qb = termQuery(
    "name",
    "kimchy"
);

6) Terms Query
QueryBuilder qb = termsQuery("tags",
    "blue", "pill");

필드명 , 검색어1,검색어2

7) Range Query

QueryBuilder qb = rangeQuery("price")
    .from(5)
    .to(10)
    .includeLower(true)
    .includeUpper(false);

8) Prefix Query
QueryBuilder qb = prefixQuery(
    "brand",
    "heine"
);

9) Wildcard Query
QueryBuilder qb = wildcardQuery("user", "k?mc*");

10) Bool Query
QueryBuilder qb = boolQuery()
    .must(termQuery("content", "test1"))
    .must(termQuery("content", "test4"))
    .mustNot(termQuery("content", "test2"))
    .should(termQuery("content", "test3"))
    .filter(termQuery("content", "test5"));

11) Dis Max Query
QueryBuilder qb = disMaxQuery()
    .add(termQuery("name", "kimchy"))
    .add(termQuery("name", "elasticsearch"))
    .boost(1.2f)
    .tieBreaker(0.7f);


12) Function Score Query

FilterFunctionBuilder[] functions = {
        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                matchQuery("name", "kimchy"),
                randomFunction("ABCDEF")),
        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                exponentialDecayFunction("age", 0L, 1L))
};
QueryBuilder qb = QueryBuilders.functionScoreQuery(functions);


13) Boosting Query
QueryBuilder qb = boostingQuery(
        termQuery("name","kimchy"),
        termQuery("name","dadoonet"))
    .negativeBoost(0.2f);


14) Geo Distance Query
QueryBuilder qb = geoDistanceQuery("pin.location")
    .point(40, -70)
    .distance(200, DistanceUnit.KILOMETERS);

15) Geo Polygon Query
List<GeoPoint> points = new ArrayList<>();
points.add(new GeoPoint(40, -70));
points.add(new GeoPoint(30, -80));
points.add(new GeoPoint(20, -90));

QueryBuilder qb =
        geoPolygonQuery("pin.location", points);
 */
@Service
public class SearchServiceImpl implements SearchService {
    @Override
    public SearchResponse select(QueryParam queryParam) throws Exception {

        /****/



        LevenshteinDistance lcsd = new LevenshteinDistance();
        int countLcsd = lcsd.apply("New York", "New Hampshire");


        /****/



        Settings settings = Settings.builder()
                .put("cluster.name", "my-application").build();
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("13.124.79.249"), 9300))
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("52.78.190.237"), 9300));

        /*하일라이팅 준비*/
        HighlightBuilder highlightBuilder = new HighlightBuilder()
                .postTags("</b>")
                .preTags("<b>")
//                .fragmentSize(100)
//                .numOfFragments(3)
                .field("title",20,3)
                .field("content",30,3);


        QueryBuilder qb1 = null;
        if (queryParam.getQ() == null) {
            qb1 = QueryBuilders.matchAllQuery();
        } else {
            qb1 = termQuery(
                    "title",
                    queryParam.getQ()
            );
        }

//        QueryBuilder qb2 = multiMatchQuery(
//                queryParam.getQ(),
//                "title","content","author"
//        );
//        qb2.boost(10F);


        //QueryBuilder postFilter = termQuery("rate",queryParam.getRateFacet());
        SearchResponse response = client.prepareSearch("foodblog", "foodblog")
                .setTypes("post", "post")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                //.setQuery(QueryBuilders.matchAllQuery())
                .highlighter(highlightBuilder)
                .suggest(new SuggestBuilder().addSuggestion("my-suggestion", SuggestBuilders.termSuggestion("spell").text(queryParam.getQ()).size(5)))
                .addAggregation(
                        AggregationBuilders.terms("author_agg").field("author")
                        //.subAggregation() bra bra bra
                ).addAggregation(
                        AggregationBuilders.terms("rate_agg").field("rate")
                        //.subAggregation() bra bra bra
                )
                .setQuery(qb1)
                //.setPostFilter(postFilter)
                //.setQuery(QueryBuilders.termsQuery("title", queryParam.getQ()))                 // Query
                //.setPostFilter(QueryBuilders.rangeQuery("age").from(12).to(18))     // Filter
                .setFrom(queryParam.getFrom())
                .setSize(queryParam.getSize())
                .setExplain(true)
                //.addSort(queryParam.getSort(), SortOrder.DESC)
                .get();

        return response;

    }

    @Override
    public SearchResponse autocompleteSelect(String query) throws Exception {

        Settings settings = Settings.builder()
                .put("cluster.name", "my-application").build();

        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("52.79.91.255"), 9300))
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("52.79.91.255"), 9300));

        SearchResponse response = client.prepareSearch("foodblog")
                .setTypes("autocomplete")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.termQuery("suggest", query))                 // Query
                //.setPostFilter(QueryBuilders.rangeQuery("age").from(12).to(18))     // Filter
                .setFrom(0).setSize(10).setExplain(false)
                .get();

        return response;
    }



    public SearchResponse selectStore(QueryParam queryParam) throws Exception {

        Settings settings = Settings.builder()
                .put("cluster.name", "my-application").build();
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("52.79.91.255"), 9300))
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("52.79.91.255"), 9300));

//        /*하일라이팅 준비*/
        HighlightBuilder highlightBuilder = new HighlightBuilder()
                .postTags("</b>")
                .preTags("<b>")
//                .fragmentSize(100)
//                .numOfFragments(3)
                .field("title",20,3)
                .field("description",30,3);


        String[] points = new String[2];
        if (queryParam.getPt() == null || queryParam.getPt().equals("")) {
            points[0] = "37.56570958096572";
            points[1] = "126.9032893177229";
        } else {
            points = queryParam.getPt().split(",");
        }
        QueryBuilder qb = geoDistanceQuery("location")
                .point(Double.parseDouble(points[0]),Double.parseDouble(points[1]))
                .distance(queryParam.getD(), DistanceUnit.KILOMETERS);


        SearchResponse response = client.prepareSearch("store")
                .setTypes("geo")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchAllQuery())
                .setPostFilter(qb)
                .setFrom(0)
                .setSize(100)
                .setExplain(true)
                .get();

        return response;
    }


    @Override
    public String getPaging(String targetUrl, String q, Long totalCnt, QueryParam queryParam) throws Exception {
            String paramStr = "";
            String htmlPage = "";
            String logUrl = "";
            int rows=10;
            int page=10;
            int targetPageValue;
            int pagingMin;
            int pageSeed;
            int pageBlock = page;
            int countInPage = page;
            int loopCnt = 0;
            int currentPage = 1;
            int start =queryParam.getFrom();

            String sort=queryParam.getSort();
            try{
                double totalPage;
                switch (rows){
                    case 8:
                        currentPage = start/8;
                        break;
                    case 16:
                        currentPage = start/16;
                        break;
                    case 24:
                        currentPage = start/24;
                        break;

                    case 10:
                        currentPage = start/10;
                        break;
                    case 20:
                        currentPage = start/20;
                        break;
                    case 50:
                        currentPage = start/50;
                        break;
                    case 100:
                        currentPage = start/100;
                        break;
                    default:
                        currentPage = start/rows;
                        break;
                }

                pagingMin = ((currentPage - 1) / countInPage) * countInPage;
                if (currentPage > 5) {
                    pageSeed = currentPage - 4;
                }
                else {
                    pageSeed = pagingMin;
                }

                double flTotalPage = Float.parseFloat(totalCnt.toString())/rows;

                totalPage = Math.ceil(flTotalPage);

                paramStr = "q="+q;

//				htmlPage = htmlPage + "<div id=\"prediv\" style=\"position:relative;display:none;left:15px;margin-left:0px;bottom:1px;z-index:1; height:0px;width:0px;\"><img width=\"10\" height=\"8\" src=\"img/select_arrow.gif\"></div>";
                if (currentPage < 1) {          //10개 짜리 이미지
                    targetPageValue = currentPage - 1;
                    logUrl = targetUrl + "?" + paramStr + "&start=" + (start - rows) + "&sort=" + sort;
                    htmlPage += "<li class=\"c-prev\"><a href=\""+logUrl+"\"><i class=\"fa fa-angle-left\"></i></a></li> ";
                }

                while (loopCnt < pageBlock && pageSeed < totalPage) {
                    if (pageSeed == currentPage) {
                        targetPageValue = pageSeed;
                        htmlPage += "<li class=\"c-active\"><a href=\"#\">" + (targetPageValue+1) + "</a></li>";
                    }
                    else {
                        targetPageValue = pageSeed;
                        logUrl = targetUrl + "?" + paramStr + "&start=" + targetPageValue * rows + "&sort=" + sort;
                        htmlPage += "<li><a href=\"" + logUrl + "\">" + (targetPageValue+1) + "</a></li>";

                    }
                    pageSeed++;
                    loopCnt++;
                }
                if (currentPage < totalPage-1) {          //10개 짜리 이미지
                    targetPageValue = pageSeed;
                    logUrl = targetUrl + "?" + paramStr + "&start=" + (start + rows) + "&sort=" + sort;
                    htmlPage += "<li class=\"c-next\"><a href=\""+logUrl+"\"><i class=\"fa fa-angle-right\"></i></a></li> ";
                } else {
                    htmlPage += "<li></li> ";
                }

                return htmlPage;
            }catch(Exception e){
                System.out.println("ResultPageFunction - GetPage : " + e.toString());
                return "";
            }
        }
}
