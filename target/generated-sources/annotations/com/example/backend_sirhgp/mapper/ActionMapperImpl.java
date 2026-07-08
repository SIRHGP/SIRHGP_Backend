package com.example.backend_sirhgp.mapper;

import com.example.backend_sirhgp.dto.response.ActionResponseDTO;
import com.example.backend_sirhgp.model.Action;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-08T06:06:42+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Microsoft)"
)
@Component
public class ActionMapperImpl implements ActionMapper {

    @Override
    public ActionResponseDTO toResponseDTO(Action action) {
        if ( action == null ) {
            return null;
        }

        ActionResponseDTO actionResponseDTO = new ActionResponseDTO();

        actionResponseDTO.setId( action.getId() );
        actionResponseDTO.setName( action.getName() );
        actionResponseDTO.setDescription( action.getDescription() );
        actionResponseDTO.setDateCreation( action.getDateCreation() );
        actionResponseDTO.setDateModification( action.getDateModification() );

        return actionResponseDTO;
    }
}
