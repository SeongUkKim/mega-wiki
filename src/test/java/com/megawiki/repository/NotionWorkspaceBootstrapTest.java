package com.megawiki.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.megawiki.config.NotionProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotionWorkspaceBootstrapTest {

    @Mock
    private NotionApiClient notionApiClient;

    @Test
    void resolvesDataSourcesByNameAndAppliesSchema() {
        NotionProperties notionProperties = new NotionProperties();
        notionProperties.setApiToken("test-token");
        NotionDataSourceRegistry registry = new NotionDataSourceRegistry();

        when(notionApiClient.searchDataSources("MegaWiki Pages"))
                .thenReturn(List.of(new NotionApiClient.DataSourceSummary("pages-id", "MegaWiki Pages")));
        when(notionApiClient.searchDataSources("MegaWiki Questions"))
                .thenReturn(List.of(new NotionApiClient.DataSourceSummary("questions-id", "MegaWiki Questions")));

        NotionWorkspaceBootstrap bootstrap = new NotionWorkspaceBootstrap(notionApiClient, notionProperties, registry);
        bootstrap.initialize();

        assertThat(registry.getPagesDataSourceId()).isEqualTo("pages-id");
        assertThat(registry.getQuestionsDataSourceId()).isEqualTo("questions-id");
        verify(notionApiClient).updateDataSource(eq("pages-id"), anyMap());
        verify(notionApiClient).updateDataSource(eq("questions-id"), anyMap());
    }

    @Test
    void prefersExplicitDataSourceIdsWhenProvided() {
        NotionProperties notionProperties = new NotionProperties();
        notionProperties.setApiToken("test-token");
        notionProperties.setPagesDataSourceId("pages-id");
        notionProperties.setQuestionsDataSourceId("questions-id");
        NotionDataSourceRegistry registry = new NotionDataSourceRegistry();

        NotionWorkspaceBootstrap bootstrap = new NotionWorkspaceBootstrap(notionApiClient, notionProperties, registry);
        bootstrap.initialize();

        assertThat(registry.getPagesDataSourceId()).isEqualTo("pages-id");
        assertThat(registry.getQuestionsDataSourceId()).isEqualTo("questions-id");
        verify(notionApiClient, never()).searchDataSources(anyString());
        verify(notionApiClient).updateDataSource(eq("pages-id"), anyMap());
        verify(notionApiClient).updateDataSource(eq("questions-id"), anyMap());
    }
}
