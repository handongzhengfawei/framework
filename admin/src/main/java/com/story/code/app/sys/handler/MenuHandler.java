package com.story.code.app.sys.handler;

import static com.story.code.common.ApiResponseVO.defaultSuccessful;

import com.story.code.app.sys.command.MenuPersistCommand;
import com.story.code.app.sys.converter.MenuAppConverter;
import com.story.code.app.sys.query.MenuPageListQuery;
import com.story.code.app.sys.validator.MenuPersistValidator;
import com.story.code.app.sys.vo.MenuPageListVO;
import com.story.code.boot.security.SecurityUtils;
import com.story.code.boot.security.TenantIdUtil;
import com.story.code.common.ApiResponseVO;
import com.story.code.common.page.reactive.PageReactiveWrapper;
import com.story.code.component.DataPersistComponent;
import com.story.code.component.page.vo.PageVO;
import com.story.code.domain.sys.factory.UserAuthorityFactory;
import com.story.code.domain.sys.valueobject.MenuTreeNode;
import com.story.code.infrastructure.tunnel.dataobject.sys.ResourceMenuDO;
import com.story.code.infrastructure.tunnel.datatunnel.ResourceMenuTunnelI;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @author storys.zhang@gmail.com
 * <p>
 * Created at 2020/5/7 by Storys.Zhang
 */
@Component
public class MenuHandler {

    @Autowired
    private ResourceMenuTunnelI resourceMenuTunnel;
    @Autowired
    private MenuAppConverter menuConverter;
    @Autowired
    private UserAuthorityFactory userAuthorityFactory;
    @Autowired
    private MenuPersistValidator menuPersistValidator;


    public Mono<ServerResponse> authority(ServerRequest request) {
        return SecurityUtils.getUserId().map(userId -> userAuthorityFactory.userAuthorityAggregation(userId).getMenuTreeNodeList())
            .flatMap(data -> ServerResponse.ok().bodyValue(ApiResponseVO.<List<MenuTreeNode>>create().data(data).buildSuccess()));
    }

    public Mono<ServerResponse> page(ServerRequest request) {

        return request.bodyToMono(MenuPageListQuery.class).flatMap(query -> {
            Mono<PageVO<MenuPageListVO>> page = PageReactiveWrapper
                .page(resourceMenuTunnel::pageList, menuConverter::doToVo, query.getPage(), menuConverter.toParam(query));
            return page;
        }).flatMap(page -> ServerResponse.ok().bodyValue(ApiResponseVO.<PageVO<MenuPageListVO>>create().data(page).buildSuccess()));
    }

    public Mono<ServerResponse> add(ServerRequest request) {
        return request.bodyToMono(MenuPersistCommand.class).flatMap(command -> {
            DataPersistComponent<MenuPersistCommand, ResourceMenuDO> component = new DataPersistComponent(command, command.getId());
            component.addValidatorFunction(menuPersistValidator::validate);
            component.addCreatePersistStrategyFunction(() -> {
                ResourceMenuDO data = new ResourceMenuDO();
                data.setTenantId(TenantIdUtil.getTenantId());
                data.setParentId(command.getParentId());
                data.setRankNum(command.getRankNum());
                data.setTitle(command.getTitle());
                data.setUrl(command.getUrl());
                data.setIcon(command.getIcon());
                return resourceMenuTunnel.create(data);
            });
            component.addUpdatePersistStrategyFunction(() -> {
                return resourceMenuTunnel.get(command.getId()).flatMap(data -> {
                    data.setParentId(command.getParentId());
                    data.setRankNum(command.getRankNum());
                    data.setTitle(command.getTitle());
                    data.setUrl(command.getUrl());
                    data.setIcon(command.getIcon());
                    return resourceMenuTunnel.update(data);
                });
            });
            return component.persist();
        })
            .flatMap(count -> ServerResponse.ok().bodyValue(defaultSuccessful()));
    }


}
